package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.*;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.service.BidNotificationService;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.event.helper.ProductChangeTracker;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsData;
import com.backend.global.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BidService {
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final WebSocketService webSocketService;
    private final BidNotificationService bidNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<Long, Object> productLocks = new ConcurrentHashMap<>();

    // ======================================= create methods ======================================= //
    public RsData<BidResponseDto> createBid(Long productId, Long bidderId, BidRequestDto request) {
        // 상품별 락 객체 가져오기 (없으면 생성)
        Object lock = productLocks.computeIfAbsent(productId, k -> new Object());

        // 동시성 제어: 같은 상품에 대한 입찰은 순차적으로 처리
        synchronized (lock) {
            return createBidInternal(productId, bidderId, request);
        }
    }

    private RsData<BidResponseDto> createBidInternal(Long productId, Long bidderId, BidRequestDto request) {
        // Product/Member 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ServiceException.notFound("존재하지 않는 상품입니다."));
        Member member = memberRepository.findById(bidderId)
                .orElseThrow(() -> ServiceException.notFound("존재하지 않는 사용자입니다."));

        // 유효성 검증
        validateBid(product, member, request.price());

        // 입찰 생성 및 저장
        Bid savedBid = saveBid(product, member, request.price());

        // 상품 업데이트
        updateProduct(product, savedBid, request.price());

        // 응답 생성
        BidResponseDto bidResponse = createBidResponse(savedBid);

        // 실시간 브로드캐스트
        webSocketService.broadcastBidUpdate(productId, bidResponse);

        // 입찰 성공 알림
        bidNotificationService.notifyBidSuccess(bidderId, product, request.price());

        return RsData.created("입찰이 완료되었습니다.", bidResponse);
    }

    private Bid saveBid(Product product, Member member, Long bidPrice) {
        Bid bid = Bid.builder()
                .bidPrice(bidPrice)
                .status(BidStatus.BIDDING)
                .product(product)
                .member(member)
                .build();
        return bidRepository.save(bid);
    }

    // ======================================= find/get methods ======================================= //
    @Transactional(readOnly = true)
    public RsData<BidCurrentResponseDto> getBidStatus(long productId) {
        // 상품 존재 확인
        Product product = getProductById(productId);

        // 현재 최고 입찰가
        Long currentPrice = bidRepository.findHighestBidPrice(productId).orElse(0L);

        // 입찰 개수
        Integer bidCount = bidRepository.countProductBid(productId);

        // 최근 입찰 내역 (상위 5개)
        List<Bid> recentBids = bidRepository.findNBids(productId, 5);

        // 익명화된 최근 입찰 목록
        List<BidCurrentResponseDto.RecentBid> recentBidList = createAnonymizedRecentBids(recentBids);

        // 응답 생성
        BidCurrentResponseDto response = new BidCurrentResponseDto(
                productId,
                product.getProductName(),
                currentPrice,
                product.getInitialPrice(),
                bidCount,
                product.getStatus(),
                product.getEndTime(),
                recentBidList
        );

        return RsData.ok("입찰 현황이 조회되었습니다.", response);
    }

    @Transactional(readOnly = true)
    public RsData<MyBidResponseDto> getMyBids(Long memberId, int page, int size) {
        // 페이지 설정
        Pageable pageable = PageRequest.of(page, size);

        // 내 입찰내역 조회
        Page<Bid> bidPage = bidRepository.findMyBids(memberId, pageable);

        // 빈 결과 처리
        if (bidPage.isEmpty()) {
            return createEmptyMyBidsResponse(page, size);
        }

        // 상품별 현재 최고가 조회
        Map<Long, Long> currentPricesMap = getCurrentPricesMap(bidPage);

        // 응답 데이터 생성
        List<MyBidResponseDto.MyBidItem> myBidItems = createMyBidItems(bidPage, currentPricesMap);

        MyBidResponseDto response = new MyBidResponseDto(
                myBidItems,
                (int) bidPage.getTotalElements(),
                bidPage.getTotalPages(),
                bidPage.getNumber(),
                bidPage.getSize(),
                bidPage.hasNext()
        );

        return RsData.ok("내 입찰 내역이 조회되었습니다.", response);
    }

    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ServiceException.notFound("존재하지 않는 상품입니다."));
    }

    private List<BidCurrentResponseDto.RecentBid> createAnonymizedRecentBids(List<Bid> recentBids) {
        AtomicInteger counter = new AtomicInteger(1);
        return recentBids.stream()
                .map(bid -> new BidCurrentResponseDto.RecentBid(
                        bid.getBidPrice(),
                        bid.getCreateDate(),
                        "익명" + counter.getAndIncrement()
                ))
                .toList();
    }

    private RsData<MyBidResponseDto> createEmptyMyBidsResponse(int page, int size) {
        MyBidResponseDto emptyBids = new MyBidResponseDto(
                List.of(), 0, 0, page, size, false
        );
        return RsData.ok("내 빈 입찰내역 조회 성공.", emptyBids);
    }

    private Map<Long, Long> getCurrentPricesMap(Page<Bid> bidPage) {
        Set<Long> productIds = bidPage.getContent().stream()
                .map(bid -> bid.getProduct().getId())
                .collect(Collectors.toSet());

        return bidRepository.findCurrentPricesForProducts(productIds).stream()
                .collect(Collectors.toMap(
                        ProductCurrentPriceDto::getProductId,
                        ProductCurrentPriceDto::getCurrentPrice
                ));
    }

    private List<MyBidResponseDto.MyBidItem> createMyBidItems(Page<Bid> bidPage, Map<Long, Long> currentPricesMap) {
        return bidPage.getContent().stream()
                .map(bid -> {
                    Product product = bid.getProduct();
                    Long currentHighestPrice = currentPricesMap.getOrDefault(product.getId(), 0L);
                    boolean isWinning = bid.getBidPrice().equals(currentHighestPrice);

                    MyBidResponseDto.SellerInfo sellerInfo = null;
                    if (product.getSeller() != null) {
                        sellerInfo = new MyBidResponseDto.SellerInfo(
                                product.getSeller().getId(),
                                product.getSeller().getNickname()
                        );
                    }

                    return new MyBidResponseDto.MyBidItem(
                            bid.getId(),
                            product.getId(),
                            product.getProductName(),
                            product.getThumbnail(),
                            bid.getBidPrice(),
                            currentHighestPrice,
                            bid.getStatus(),
                            isWinning,
                            bid.getCreateDate(),
                            product.getEndTime(),
                            product.getStatus(),
                            sellerInfo
                    );
                })
                .toList();
    }

    // ======================================= validation methods ======================================= //
    private void validateBid(Product product, Member member, Long bidPrice) {
        // 경매 상태 확인
        validateAuctionStatus(product);

        // 경매 시간 확인
        validateAuctionTime(product);

        // 본인 상품 입찰 방지
        validateNotSelfBid(product, member);

        // 입찰 금액 유효성 검증
        validateBidPrice(bidPrice, product);
    }

    private void validateAuctionStatus(Product product) {
        if (!AuctionStatus.BIDDING.getDisplayName().equals(product.getStatus())) {
            throw ServiceException.badRequest("현재 입찰할 수 없는 상품입니다.");
        }
    }

    private void validateAuctionTime(Product product) {
        LocalDateTime now = LocalDateTime.now();
        if (product.getStartTime() != null && now.isBefore(product.getStartTime())) {
            throw ServiceException.badRequest("경매가 아직 시작되지 않았습니다.");
        }
        if (product.getEndTime() != null && now.isAfter(product.getEndTime())) {
            throw ServiceException.badRequest("경매가 이미 종료되었습니다.");
        }
    }

    private void validateNotSelfBid(Product product, Member member) {
        Member seller = product.getSeller();
        if (seller != null && seller.getId().equals(member.getId())) {
            throw ServiceException.badRequest("본인이 등록한 상품에는 입찰할 수 없습니다.");
        }
    }

    private void validateBidPrice(Long bidPrice, Product product) {
        // 입찰 금액 기본 검증
        if (bidPrice == null || bidPrice <= 0) {
            throw ServiceException.badRequest("입찰 금액은 0보다 커야 합니다.");
        }

        // 현재 최고가보다 높은지 확인
        Long currentHighestPrice = bidRepository.findHighestBidPrice(product.getId()).orElse(0L);
        if (bidPrice <= currentHighestPrice) {
            throw ServiceException.badRequest("입찰 금액이 현재 최고가인 " + currentHighestPrice + "원 보다 높아야 합니다.");
        }

        // 최소 입찰단위 100원
        if (bidPrice % 100 != 0) {
            throw ServiceException.badRequest("입찰 금액은 100원 단위로 입력해주세요.");
        }

        // 최소 입찰단위 지켰는지 확인
        if (bidPrice < currentHighestPrice + 100) {
            throw ServiceException.badRequest("최소 100원이상 높게 입찰해주세요.");
        }
    }

    // ======================================= update methods ======================================= //
    private void updateProduct(Product product, Bid savedBid, Long newPrice) {
        ProductChangeTracker tracker = ProductChangeTracker.of(product);

        product.addBid(savedBid);
        product.setCurrentPrice(newPrice);

        tracker.publishChanges(eventPublisher, product);
    }

    // ======================================= helper methods ======================================= //
    private BidResponseDto createBidResponse(Bid bid) {
        return new BidResponseDto(
                bid.getId(),
                bid.getProduct().getId(),
                bid.getMember().getId(),
                bid.getBidPrice(),
                bid.getStatus(),
                bid.getCreateDate()
        );
    }
}
