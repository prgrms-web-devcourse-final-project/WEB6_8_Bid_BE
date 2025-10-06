package com.backend.domain.cash.service;

import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.enums.CashTxType;
import com.backend.domain.cash.enums.RelatedType;
import com.backend.domain.cash.dto.CashResponse;
import com.backend.domain.cash.dto.CashTransactionItemResponse;
import com.backend.domain.cash.dto.CashTransactionResponse;
import com.backend.domain.cash.dto.CashTransactionsResponse;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.cash.repository.CashTransactionRepository;
import com.backend.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashService {

    private final CashRepository cashRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final BidRepository bidRepository;

    @Transactional(readOnly = true)
    public CashResponse getMyCashResponse(Member member) {
        Cash cash = cashRepository.findByMember(member)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "지갑이 아직 생성되지 않았습니다.")
                );

        return CashResponse.builder()
                .cashId(cash.getId())
                .memberId(member.getId())
                .balance(cash.getBalance())
                .createDate(cash.getCreateDate() != null ? cash.getCreateDate().toString() : null)
                .modifyDate(cash.getModifyDate() != null ? cash.getModifyDate().toString() : null)
                .build();
    }

    // 원장 목록 조회..
    @Transactional(readOnly = true)
    public CashTransactionsResponse getMyTransactions(Member member, int page1Base, int size) {
        Cash cash = cashRepository.findByMember(member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지갑이 아직 생성되지 않았습니다."));

        int page0 = Math.max(0, page1Base - 1);
        PageRequest pageable = PageRequest.of(page0, Math.max(1, size));

        Page<CashTransaction> page = cashTransactionRepository.findAllByCashOrderByIdDesc(cash, pageable);

        List<CashTransactionItemResponse> items = page.getContent().stream()
                .map(this::toItem)
                .toList();

        return CashTransactionsResponse.builder()
                .page(page1Base)
                .size(size)
                .total(page.getTotalElements())
                .items(items)
                .build();
    }

    private CashTransactionItemResponse toItem(CashTransaction tx) {
        String createdAt = tx.getCreateDate() != null
                ? tx.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        // 관련 정보(ERD의 related_type / related_id 기반)..
        CashTransactionItemResponse.Related related = null;
        if (tx.getRelatedType() != null && tx.getRelatedId() != null) {
            related = CashTransactionItemResponse.Related.builder()
                    .type(tx.getRelatedType().name())
                    .id(tx.getRelatedId())
                    .build();
        }

        return CashTransactionItemResponse.builder()
                .transactionId(tx.getId())
                .cashId(tx.getCash().getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .createdAt(createdAt)
                .related(related)
                .build();
    }

    // 원장 단건 상세..
    @Transactional(readOnly = true)
    public CashTransactionResponse getMyTransactionDetail(Member member, Long transactionId) {

        // 내 지갑..
        Cash cash = cashRepository.findByMember(member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지갑이 아직 생성되지 않았습니다."));

        // 내 지갑에 속한 트랜잭션만 조회 (타인의 데이터 차단)..
        CashTransaction tx = cashTransactionRepository.findByIdAndCash(transactionId, cash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "원장 내역을 찾을 수 없습니다."));

        String createdAt = tx.getCreateDate() != null
                ? tx.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        // related + 링크(선택)..
        CashTransactionResponse.Related related = null;
        if (tx.getRelatedType() != null && tx.getRelatedId() != null) {
            CashTransactionResponse.Links links = buildLinks(tx.getRelatedType(), tx.getRelatedId());

            related = CashTransactionResponse.Related.builder()
                    .type(tx.getRelatedType().name())
                    .id(tx.getRelatedId())
                    .links(links)
                    .build();
        }

        return CashTransactionResponse.builder()
                .transactionId(tx.getId())
                .cashId(tx.getCash().getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .createdAt(createdAt)
                .related(related)
                .build();
    }

    private CashTransactionResponse.Links buildLinks(RelatedType type, Long id) {
        if (id == null) return null;
        return switch (type) {
            case PAYMENT -> CashTransactionResponse.Links.builder()
                    .paymentDetail("/api/v1/payments/me/" + id)
                    .build();
            case BID -> {
                Long productId = bidRepository.findById(id)
                        .map(b -> b.getProduct().getId())
                        .orElse(null);
                yield productId == null ? null :
                        CashTransactionResponse.Links.builder()
                                .bidDetail("/api/v1/bids/products/" + productId)
                                .build();
            }
            default -> null; // REFUND/ADJUSTMENT/PROMOTION 등은 링크 없을 수 있음..
        };
    }

    /**
     * 지갑에서 돈을 빼고, 원장(WITHDRAW) 한 줄을 남깁니다.
     * 예) 낙찰 결제면: withdraw(me, finalPrice, RelatedType.BID, bidId)
     */
    @Transactional
    public CashTransaction withdraw(Member member, long amount,
                                    RelatedType relatedType, Long relatedId) {

        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "출금 금액은 0보다 커야 합니다.");
        }

        // 내 지갑을 '잠그고' 가져오기 (동시에 두 번 빼는 문제 방지)..
        var cash = cashRepository.findWithLockByMember(member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지갑이 아직 생성되지 않았습니다."));

        long current = cash.getBalance() == null ? 0L : cash.getBalance();

        // 잔액 부족이면 실패..
        if (current < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잔액이 부족합니다.");
         }

        // 잔액 차감..
        long newBalance = current - amount;
        cash.setBalance(newBalance);

        // 원장(WITHDRAW) 한 줄 추가..
        var tx = CashTransaction.builder()
                .cash(cash)
                .type(CashTxType.WITHDRAW)  // 출금..
                .amount(amount)             // 항상 양수로 저장..
                .balanceAfter(newBalance)   // 이 줄 반영 후 잔액..
                .relatedType(relatedType)   // 돈이 왜 빠졌는지..
                .relatedId(relatedId)       // 관련 ID (예: bidId)..
                .build();

        return cashTransactionRepository.save(tx);
    }
}
