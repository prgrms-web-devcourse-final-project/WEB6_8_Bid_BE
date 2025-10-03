package com.backend.domain.bid.service;

import com.backend.domain.bid.dto.BidPayResponseDto;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.service.CashService;
import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class BidPaymentService {
    private final BidRepository bidRepository;
    private final CashService cashService;

    public RsData<BidPayResponseDto> payForBid(Long memberId, Long bidId) {
        // 입찰 조회
        Bid bid = getBidById(bidId);
        Product product = bid.getProduct();
        Member bidder = bid.getMember();

        // 유효성 검증
        validatePayment(memberId, bid, product);

        // 이미 결제했는지 확인 (멱등 처리)
        if (bid.getPaidAt() != null) {
            return createAlreadyPaidResponse(bid, product);
        }

        // 최고가 재검증
        validateHighestBid(bid, product);

        // 결제 처리
        return processPayment(bid, bidder, product);
    }

    // ======================================= find/get methods ======================================= //
    private Bid getBidById(Long bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ServiceException("404", "입찰을 찾을 수 없습니다."));
    }

    // ======================================= validation methods ======================================= //
    private void validatePayment(Long memberId, Bid bid, Product product) {
        // 내가 한 입찰인지 확인
        Member bidder = bid.getMember();
        if (bidder == null || !bidder.getId().equals(memberId)) {
            throw new ServiceException("403", "내 입찰만 결제할 수 있습니다.");
        }

        // 상품이 '낙찰' 상태인지 확인
        String successfulStatus = AuctionStatus.SUCCESSFUL.getDisplayName();
        if (product == null || product.getStatus() == null || !successfulStatus.equals(product.getStatus())) {
            throw new ServiceException("400", "아직 낙찰이 확정되지 않았습니다.");
        }
    }

    private void validateHighestBid(Bid bid, Product product) {
        Long highest = bidRepository.findHighestBidPrice(product.getId()).orElse(0L);
        if (!bid.getBidPrice().equals(highest)) {
            throw new ServiceException("400", "현재 낙찰가와 일치하지 않습니다. 다시 확인해주세요.");
        }
    }

    // ======================================= payment methods ======================================= //
    private RsData<BidPayResponseDto> processPayment(Bid bid, Member bidder, Product product) {
        Long finalPrice = bid.getBidPrice();

        // 출금
        var tx = cashService.withdraw(
                bidder,
                finalPrice,
                com.backend.domain.cash.constant.RelatedType.BID,
                bid.getId()
        );

        // 결제 기록
        bid.setPaidAt(LocalDateTime.now());
        bid.setPaidAmount(finalPrice);

        // 응답
        BidPayResponseDto response = new BidPayResponseDto(
                bid.getId(),
                product.getId(),
                finalPrice,
                bid.getPaidAt(),
                tx.getId(),
                tx.getBalanceAfter()
        );

        return RsData.ok("낙찰 결제가 완료되었습니다.", response);
    }

    private RsData<BidPayResponseDto> createAlreadyPaidResponse(Bid bid, Product product) {
        BidPayResponseDto response = new BidPayResponseDto(
                bid.getId(),
                product.getId(),
                bid.getPaidAmount(),
                bid.getPaidAt(),
                null,
                null
        );
        return RsData.ok("이미 결제된 입찰입니다.", response);
    }
}
