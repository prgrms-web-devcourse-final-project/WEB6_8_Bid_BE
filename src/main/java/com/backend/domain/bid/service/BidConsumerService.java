package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidMessageDto;
import com.backend.domain.bid.dto.BidResponseDto;
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
import com.backend.global.lock.DistributedLock;
import com.backend.global.websocket.service.WebSocketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidConsumerService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidRepository bidRepository;
    private final WebSocketService webSocketService;
    private final BidNotificationService bidNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 100) // 0.1초마다 실행
    public void consumeBidQueue() {
        String messageJson = redisTemplate.opsForList().leftPop("bid_queue");

        if (messageJson != null) {
            try {
                BidMessageDto messageDto = objectMapper.readValue(messageJson, BidMessageDto.class);
                processBid(messageDto);
            } catch (JsonProcessingException e) {
                log.error("입찰 메시지 역직렬화 실패: {}", messageJson, e);
            } catch (Exception e) {
                log.error("입찰 처리 중 예외 발생: {}", e.getMessage(), e);
                // 예외 발생 시, 해당 메시지를 별도의 Dead Letter Queue로 보내거나 로깅 후 폐기하는 정책 필요
            }
        }
    }

    @DistributedLock(key = "'product:' + #messageDto.productId", waitTime = 10, leaseTime = 10)
    public void processBid(BidMessageDto messageDto) {
        Long productId = messageDto.getProductId();
        Long bidderId = messageDto.getBidderId();
        Long price = messageDto.getPrice();

        // Product/Member 조회
        Product product = productRepository.findByIdWithBids(productId)
                .orElseThrow(() -> ServiceException.notFound("존재하지 않는 상품입니다."));
        Member member = memberRepository.findById(bidderId)
                .orElseThrow(() -> ServiceException.notFound("존재하지 않는 사용자입니다."));

        // 유효성 검증
        validateBid(product, member, price);

        Long previousHighestPrice = bidRepository.findHighestBidPrice(productId).orElse(null);

        // 이전 최고 입찰자 확인 (입찰 밀림 알림용)
        Bid previousHighestBid = null;
        if (previousHighestPrice != null) {
            List<Bid> recentBids = bidRepository.findNBids(productId, 10);
            previousHighestBid = recentBids.stream()
                    .filter(bid -> bid.getBidPrice().equals(previousHighestPrice))
                    .findFirst()
                    .orElse(null);
        }

        // 입찰 생성 및 저장
        Bid savedBid = saveBid(product, member, price);

        // 상품 업데이트
        updateProduct(product, savedBid, price);

        // 응답 생성
        BidResponseDto bidResponse = createBidResponse(savedBid);

        // 실시간 브로드캐스트
        webSocketService.broadcastBidUpdate(productId, bidResponse);

        // 입찰 성공 알림 (현재 입찰자에게)
        bidNotificationService.notifyBidSuccess(bidderId, product, price);

        // 입찰 밀림 알림 (이전 최고 입찰자에게)
        if (previousHighestBid != null && !previousHighestBid.getMember().getId().equals(bidderId)) {
            bidNotificationService.notifyBidOutbid(
                    previousHighestBid.getMember().getId(),
                    product,
                    previousHighestBid.getBidPrice(),
                    price
            );
        }
        log.info("입찰 성공: 상품 ID {}, 입찰자 ID {}, 입찰가 {}", productId, bidderId, price);
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
        Long currentHighestPrice = bidRepository.findHighestBidPrice(product.getId()).orElse(product.getInitialPrice());
        if (bidPrice <= currentHighestPrice) {
            throw ServiceException.badRequest("입찰 금액이 현재 최고가인 " + currentHighestPrice + "원 보다 높아야 합니다.");
        }

        // 최소 입찰단위 100원
        if (bidPrice % 100 != 0) {
            throw ServiceException.badRequest("입찰 금액은 100원 단위로 입력해주세요.");
        }
    }

    private void updateProduct(Product product, Bid savedBid, Long newPrice) {
        ProductChangeTracker tracker = ProductChangeTracker.of(product);

        product.addBid(savedBid);
        product.setCurrentPrice(newPrice);

        productRepository.save(product); // 변경사항을 명시적으로 저장

        tracker.publishChanges(eventPublisher, product);
    }

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
