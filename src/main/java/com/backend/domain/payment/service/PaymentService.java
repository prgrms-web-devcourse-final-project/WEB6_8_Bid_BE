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
import com.backend.domain.payment.dto.PgChargeResultResponse;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

    @Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;                      // 결제..
    private final PaymentMethodRepository paymentMethodRepository;          // 수단..
    private final CashRepository cashRepository;                            // 지갑..
    private final CashTransactionRepository cashTransactionRepository;      // 원장..
    private final TossBillingClient tossBillingClient;
    private static final long MIN_AMOUNT = 100L;                            // 최소 100원
    private static final long MAX_AMOUNT_PER_TX = 1_000_000L;               // 1회 한도(예시)..

    @Transactional                                                    // 원자성 보장..
    public PaymentResponse charge(Member actor, PaymentRequest req) {

        // 입력 검증..
        if (req.getAmount() == null || req.getAmount() < MIN_AMOUNT)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최소 결제 금액은 100원입니다.");

        if (req.getAmount() > MAX_AMOUNT_PER_TX)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"1회 최대 충전 한도를 초과했습니다.");

        if (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idempotencyKey가 필요합니다.");

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제수단을 찾을 수 없습니다."));

        if (Boolean.FALSE.equals(pm.getActive()) || Boolean.TRUE.equals(pm.getDeleted()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비활성/삭제된 결제수단입니다.");

        if (pm.getToken() == null || pm.getToken().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이 결제수단에는 billingKey가 없습니다. 카드 등록(빌링키 발급) 후 사용하세요.");

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

        log.info("[PM] id={}, token(billingKey)={}, brand={}, last4={}",
                pm.getId(), pm.getToken(), pm.getBrand(), pm.getLast4());

        // (PG 모의) → (실제 토스 빌링 결제)로 교체..
        String customerKey = "user-" + actor.getId();
        PgChargeResultResponse res;
        try {
            res = tossBillingClient.charge(
                    pm.getToken(),           // billingKey
                    req.getAmount(),         // 금액
                    req.getIdempotencyKey(),  // 멱등키
                    customerKey
            );
        } catch (ResponseStatusException ex) {
            // 실패 상태로 마킹(선택)
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // 4xx/5xx 그대로 클라이언트에 전달
            throw ex;
        }

        // (tossBillingClient가 onStatus 에러를 던지도록 바꿨다면 아래는 안전망 정도로 유지)
        if (!res.isSuccess()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionId(res.getTransactionId()); // 있을 수도/없을 수도
            paymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PG 승인 실패: " + res.getFailureMsg());
        }

        // 성공: PG에서 받은 키를 트랜잭션 ID로 저장...
        payment.setTransactionId(res.getTransactionId());

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
