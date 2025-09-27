package com.backend.domain.payment.service;

import com.backend.domain.cash.constant.CashTxType;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.cash.repository.CashTransactionRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.constant.PaymentStatus;
import com.backend.domain.payment.dto.PaymentRequest;
import com.backend.domain.payment.dto.PaymentResponse;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;                      // 결제..
    private final PaymentMethodRepository paymentMethodRepository;          // 수단..
    private final CashRepository cashRepository;                            // 지갑..
    private final CashTransactionRepository cashTransactionRepository;      // 원장..

    private static final long MAX_AMOUNT_PER_TX = 1_000_000L;               // 1회 한도(예시)..

    @Transactional                                                    // 원자성 보장..
    public PaymentResponse charge(Member actor, PaymentRequest req) {

        // 입력 검증..
        if (req.getAmount() == null || req.getAmount() <= 0)         // 금액 필수/양수..
            throw new IllegalArgumentException("금액은 1원 이상이어야 합니다.");
        if (req.getAmount() > MAX_AMOUNT_PER_TX)                     // 한도 체크..
            throw new IllegalArgumentException("1회 최대 충전 한도를 초과했습니다.");
        if (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank()) // 멱등키 필수..
            throw new IllegalArgumentException("idempotencyKey가 필요합니다.");

        // 멱등 선조회(같은 회원+키면 기존 결과 그대로 반환)..
        var existingOpt = paymentRepository.findByMemberAndIdempotencyKey(actor, req.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            var p = existingOpt.get();
            Long balanceAfter = cashRepository.findByMember(actor).map(Cash::getBalance).orElse(0L); // 최신 잔액..
            return toResponse(p, balanceAfter, null);                       // 기존 영수증 그대로..
        }

        // 결제수단 검증(본인 소유 + 삭제X + active)..
        PaymentMethod pm = paymentMethodRepository
                .findByIdAndMemberAndDeletedFalse(req.getPaymentMethodId(), actor) // 삭제 안 된 본인 수단..
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다."));
        if (Boolean.FALSE.equals(pm.getActive()))                                   // 비활성 차단..
            throw new IllegalArgumentException("비활성화된 결제수단입니다.");

        // payments: PENDING 생성..
        Payment payment = Payment.builder()
                .member(actor)                                     // 누가..
                .paymentMethod(pm)                                 // 어떤 수단으로..
                .amount(req.getAmount())                           // 금액..
                .currency("KRW")                                   // 통화 고정..
                .status(PaymentStatus.PENDING)                     // 대기 상태..
                .provider(pm.getProvider())                        // PG 스냅샷..
                .methodType(pm.getType().name())                   // "CARD"/"BANK"..
                .idempotencyKey(req.getIdempotencyKey())           // 멱등키..

                // 표시용 스냅샷..
                .methodAlias(pm.getAlias())
                .cardBrand(pm.getBrand())
                .cardLast4(pm.getLast4())
                .bankName(pm.getBankName())
                .bankLast4(pm.getAcctLast4())
                .build();
        payment = paymentRepository.save(payment);

        // (PG 모의) 승인 성공 가정 → 트랜잭션ID 발급..
        String txId = "pg_" + payment.getId() + "_" + System.currentTimeMillis(); // 간단 모의..
        payment.setTransactionId(txId);

        // 지갑 잠금 후 잔액 증가 + 원장 DEPOSIT 기록..
        Cash cash = cashRepository.findWithLockByMember(actor)                     // 비관적 잠금..
                .orElseGet(() -> cashRepository.save(newCash(actor)));            // 없으면 생성(0원)..
        long newBalance = (cash.getBalance() == null ? 0L : cash.getBalance()) + req.getAmount(); // 널 대비..
        cash.setBalance(newBalance);                                              // 잔액 반영..

        CashTransaction tx = CashTransaction.builder()
                .cash(cash)
                .type(CashTxType.DEPOSIT)                                         // 입금..
                .amount(req.getAmount())
                .balanceAfter(newBalance)
                .relatedType("PAYMENT")                                           // 근거: PAYMENT..
                .relatedId(payment.getId())
                .build();
        cashTransactionRepository.save(tx);

        // payments: SUCCESS 전환 + paidAt 세팅..
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());                                  // 승인 시각..

        return toResponse(payment, newBalance, tx.getId());
    }

    private Cash newCash(Member m) {                                              // 새 지갑(0원)..
        return Cash.builder().member(m).balance(0L).build();
    }

    private PaymentResponse toResponse(Payment p, Long balanceAfter, Long cashTxId) {
        return PaymentResponse.builder()
                .paymentId(p.getId())                                   // 결제 PK(영수증 ID)..
                .paymentMethodId(p.getPaymentMethod().getId())          // 사용한 결제수단 ID..
                .status(p.getStatus().name())                           // 결제 상태("PENDING"/"SUCCESS"/...)..
                .amount(p.getAmount())                                  // 충전 금액(원)..
                .currency(p.getCurrency())                              // 통화("KRW")..
                .provider(p.getProvider())                              // PG 제공사 스냅샷("toss" 등)..
                .methodType(p.getMethodType())                          // 결제수단 타입 스냅샷("CARD"/"BANK")..
                .transactionId(p.getTransactionId())                    // PG 트랜잭션 ID(있으면)..
                .createdAt(p.getCreateDate() == null ? null :           // 결제 레코드 생성 시각..
                        p.getCreateDate().toString())
                .paidAt(p.getPaidAt() == null ? null :                  // 결제 승인 확정 시각..
                        p.getPaidAt().toString())
                .idempotencyKey(p.getIdempotencyKey())                  // 멱등키(중복요청 식별용)..
                .cashTransactionId(cashTxId)                            // 현 충전으로 생성된 원장(입금) 레코드 ID..
                //  ↳ 멱등 재호출이면 새로 만든 원장이 없으므로 null(이미 같은 거래가 처리돼서 새롭게 적을 입금 기록이 없다는 뜻)..
                .balanceAfter(balanceAfter)                             // 충전 반영 잔액..
                .build();
    }
}
