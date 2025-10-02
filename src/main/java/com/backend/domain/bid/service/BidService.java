package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.*;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.constant.RelatedType;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.service.CashService;
import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.global.exception.ServiceException;
import com.backend.domain.notification.service.BidNotificationService;
import com.backend.global.response.RsData;
import com.backend.global.websocket.service.WebSocketService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
    private final EntityManager entityManager; 
    private final WebSocketService webSocketService;
    private final BidNotificationService bidNotificationService;
    private final CashService cashService;

    // 상품별 락
    private final Map<Long, Object> productLocks = new ConcurrentHashMap<>();
    
    public RsData<BidResponseDto> createBid(Long productId, Long bidderId, BidRequestDto request){
        // 상품별 락 객체 가져오기 (없으면 생성)
        Object lock = productLocks.computeIfAbsent(productId, k -> new Object());
        
        // 동시성 제어: 같은 상품에 대한 입찰은 순차적으로 처리
        synchronized (lock) {
            return createBidInternal(productId, bidderId, request);
        }
    }
    
    private RsData<BidResponseDto> createBidInternal(Long productId, Long bidderId, BidRequestDto request) {
        // 1. Product/Member 조회
        Product product = entityManager.find(Product.class, productId);
        Member member = entityManager.find(Member.class, bidderId);
        // 2. 조회된 엔티티로 유효성 검증
        validateBid(product,member,request.price());
        // 3. 입찰 생성
        Bid bid = Bid.builder()
                .bidPrice(request.price())
                .status("bidding")   // 기존과 동일하게 문자열 유지
                .product(product)
                .member(member)
                // paidAt / paidAmount 는 결제 전이므로 비워둠(null)
                .build();
        Bid savedBid = bidRepository.save(bid);
        product.addBid(savedBid);
        // 4. 입찰가 업데이트
        product.setCurrentPrice(request.price());
        // 5. 응답 데이터 생성
        BidResponseDto bidResponse = new BidResponseDto(
                savedBid.getId(),
                productId,
                bidderId,
                savedBid.getBidPrice(),
                savedBid.getStatus(),
                savedBid.getCreateDate()
        );
        
        // 6. 실시간 브로드캐스트 추가
        webSocketService.broadcastBidUpdate(productId, bidResponse);
        
        // 7. 입찰 성공 개인 알림
        bidNotificationService.notifyBidSuccess(bidderId, product, request.price());
        
        return new RsData<>("201","입찰이 완료되었습니다.",bidResponse);

    }

    @Transactional(readOnly = true)
    public RsData<BidCurrentResponseDto> getBidStatus(long productId){
        // 1. 상품 존재 확인
        Product product = entityManager.find(Product.class, productId);
        if(product == null){
            throw new ServiceException("404", "존재하지 않는 상품입니다.");
        }
        // 2. 현재 최고 입찰가
        Long currentPrice = bidRepository.findHighestBidPrice(productId).orElse(0L);
        // 3. 입찰 개수
        Integer bidCount = bidRepository.countProductBid(productId);
        // 4. 최근 입찰 내역 (상위 5개)
        List<Bid> recentBids = bidRepository.findNBids(productId,5);
        // 5. 익명화
        AtomicInteger counter = new AtomicInteger(1);
        List<BidCurrentResponseDto.RecentBid> recentBidList = recentBids.stream()
                .map(bid -> new BidCurrentResponseDto.RecentBid(
                        bid.getBidPrice(),
                        bid.getCreateDate(),
                        "익명"+counter.getAndIncrement()
                )).toList();
        // 6. response 생성
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
        return new RsData<>("200","입찰 현황이 조회되었습니다.",response);
    }

    @Transactional(readOnly = true)
    public RsData<MyBidResponseDto> getMyBids(Long memberId, int page, int size){
        // 1. page 설정
        Pageable pageable = PageRequest.of(page, size);
        // 2. 내 입찰내역 조회
        Page<Bid> bidPage = bidRepository.findMyBids(memberId, pageable);
        if(bidPage.isEmpty()){
            MyBidResponseDto emptyBids = new MyBidResponseDto(
                    List.of(),0,0,page,size,false
            );
            return new RsData<>("200","내 빈 입찰내역 조회 성공.",emptyBids);
        }

        // BidPage에서 상품 ID만 추출
        Set<Long> productIds = bidPage.getContent().stream().map(bid -> bid.getProduct().getId()).collect(Collectors.toSet());
        // 각 상품의 현재 최고 입찰가 조회
        Map<Long, Long> currentPricesMap = bidRepository.findCurrentPricesForProducts(productIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        // 3. response 데이터 생성
        List<MyBidResponseDto.MyBidItem> myBidItems = bidPage.getContent().stream()
                .map(bid -> {
                    Product product = bid.getProduct();

                    // 각 입찰이 현재 최고가인지 확인
                    Long currentHighestPrice = currentPricesMap.getOrDefault(product.getId(), 0L);
                    boolean isWinning = bid.getBidPrice().equals(currentHighestPrice);

                    MyBidResponseDto.SellerInfo sellerInfo = null;
                    if(product.getSeller() != null){
                        sellerInfo = new MyBidResponseDto.SellerInfo(product.getSeller().getId(),product.getSeller().getNickname());
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
                }).toList();

        MyBidResponseDto response = new MyBidResponseDto(
                myBidItems,
                (int) bidPage.getTotalElements(),
                bidPage.getTotalPages(),
                bidPage.getNumber(),
                bidPage.getSize(),
                bidPage.hasNext()
        );
        return new RsData<>("200","내 입찰 내역이 조회되었습니다.",response);
    }

    private void validateBid(Product product,Member member, Long bidPrice){
        // 1. 상품 존재 확인
        if(product == null){
            throw new ServiceException("404", "존재하지 않는 상품입니다.");
        }
        if(member == null){
            throw new ServiceException("404", "존재하지 않는 사용자입니다.");
        }

        // 2. 경매 진행 상태 확인
        if(!AuctionStatus.BIDDING.getDisplayName().equals(product.getStatus())){
            throw new ServiceException("400", "현재 입찰할 수 없는 상품입니다.");
        }

        // 3. 경매시간 확인
        LocalDateTime now = LocalDateTime.now();
        if(product.getStartTime()!=null && now.isBefore(product.getStartTime())){
            throw new ServiceException("400", "경매가 아직 시작되지 않았습니다.");
        }
        if(product.getEndTime()!=null && now.isAfter(product.getEndTime())){
            throw new ServiceException("400", "경매가 이미 종료되었습니다.");
        }

        // 4. 본인이 본인상품 입찰X
        Member seller = product.getSeller();
        if(seller !=null && seller.getId() == member.getId()){
            throw new ServiceException("400","본인이 등록한 상품에는 입찰할 수 없습니다.");
        }

        // 5. 입찰 금액 유효성 검사 및 현재 최고가보다 높은지 확인
        if (bidPrice == null || bidPrice <= 0) {
            throw new ServiceException("400", "입찰 금액은 0보다 커야 합니다.");
        }

        Long currentHighestPrice = bidRepository.findHighestBidPrice(product.getId()).orElse(0L);
        if(bidPrice <= currentHighestPrice){
            throw new ServiceException("400", "입찰 금액이 현재 최고가인 "+currentHighestPrice+"원 보다 높아야 합니다.");
        }

        // 6. 최소 입찰단위 100원
        if(bidPrice % 100!=0){
            throw new ServiceException("400", "입찰 금액은 100원 단위로 입력해주세요.");
        }

        // 7. 최소 입찰단위 지켰는지 확인
        if(bidPrice < currentHighestPrice + 100){
            throw new ServiceException("400","최소 100원이상 높게 입찰해주세요.");
        }
    }

    public RsData<BidPayResponseDto> payForBid(Long memberId, Long bidId) {
        // 1) 입찰 조회
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ServiceException("404", "입찰을 찾을 수 없습니다."));

        Product product = bid.getProduct();
        Member bidder = bid.getMember();

        // 2) 내가 한 입찰인지 확인
        if (bidder == null || !bidder.getId().equals(memberId)) {
            throw new ServiceException("403", "내 입찰만 결제할 수 있습니다.");
        }

        // 3) 상품이 '낙찰' 상태인지 확인 (문자열 그대로 사용)
        String SUCCESSFUL = com.backend.domain.product.enums.AuctionStatus.SUCCESSFUL.getDisplayName(); // "낙찰"
        if (product == null || product.getStatus() == null || !SUCCESSFUL.equals(product.getStatus())) {
            throw new ServiceException("400", "아직 낙찰이 확정되지 않았습니다.");
        }

        // 4) 이미 결제했는지 확인 (멱등 처리)
        if (bid.getPaidAt() != null) {
            BidPayResponseDto resp = new BidPayResponseDto(
                    bid.getId(),
                    product.getId(),
                    bid.getPaidAmount(),
                    bid.getPaidAt(),
                    null,
                    null
            );
            return new RsData<>("200","이미 결제된 입찰입니다.", resp);
        }

        // 5) 내 입찰가가 현재 최고가인지 재검증
        Long highest = bidRepository.findHighestBidPrice(product.getId()).orElse(0L);
        if (!bid.getBidPrice().equals(highest)) {
            throw new ServiceException("400", "현재 낙찰가와 일치하지 않습니다. 다시 확인해주세요.");
        }

        Long finalPrice = bid.getBidPrice();

        // 6) 출금
        var tx = cashService.withdraw(bidder, finalPrice, com.backend.domain.cash.constant.RelatedType.BID, bid.getId());

        // 7) 결제 기록
        bid.setPaidAt(java.time.LocalDateTime.now());
        bid.setPaidAmount(finalPrice);

        // 8) 응답
        BidPayResponseDto response = new BidPayResponseDto(
                bid.getId(),
                product.getId(),
                finalPrice,
                bid.getPaidAt(),
                tx.getId(),
                tx.getBalanceAfter()
        );

        return new RsData<>("200", "낙찰 결제가 완료되었습니다.", response);
    }
}
