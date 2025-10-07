package com.backend.domain.payment.service;

import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.cash.repository.CashTransactionRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.domain.payment.dto.response.MyPaymentResponse;
import com.backend.domain.payment.dto.response.MyPaymentsResponse;
import com.backend.domain.payment.dto.response.PgChargeResultResponse;
import com.backend.domain.payment.dto.request.PaymentRequest;
import com.backend.domain.payment.dto.response.PaymentResponse;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired PaymentService paymentService;

    @Autowired MemberRepository memberRepository;
    @Autowired PaymentMethodRepository paymentMethodRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired CashRepository cashRepository;
    @Autowired CashTransactionRepository cashTransactionRepository;

    @MockBean TossBillingClientService tossBillingClientService;

    Member me;
    PaymentMethod card;

    @BeforeEach
    void setUp() {
        // 회원 (password는 NOT NULL 이므로 꼭 채움)
        me = memberRepository.save(
                Member.builder()
                        .email("pay@test.com")
                        .password("pw!")
                        .nickname("payer")
                        .build()
        );

        // 기본 카드 수단 (토큰=billingKey 필수)
        card = paymentMethodRepository.save(
                PaymentMethod.builder()
                        .member(me)
                        .type(PaymentMethodType.CARD)
                        .token("BILL-001")          // billingKey
                        .alias("테스트카드")
                        .isDefault(true)
                        .brand("SHINHAN")
                        .last4("1234")
                        .expMonth(12)
                        .expYear(2030)
                        .provider("toss")
                        .active(true)
                        .deleted(false)
                        .build()
        );
    }

    @Test
    void charge_success_지갑입금_원장생성() {
        // given: 토스 빌링 성공 응답
        given(tossBillingClientService.charge(
                ArgumentMatchers.eq("BILL-001"),
                ArgumentMatchers.eq(5_000L),
                ArgumentMatchers.eq("idem-1"),
                ArgumentMatchers.eq("user-" + me.getId())
        )).willReturn(PgChargeResultResponse.builder()
                .success(true)
                .transactionId("PAY-123")
                .build());

        // when
        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(card.getId());
        req.setAmount(5_000L);
        req.setIdempotencyKey("idem-1");

        PaymentResponse res = paymentService.charge(me, req);

        // then: 응답/영수증
        assertThat(res.getStatus()).isEqualTo("SUCCESS");
        assertThat(res.getAmount()).isEqualTo(5_000L);
        assertThat(res.getTransactionId()).isEqualTo("PAY-123");
        assertThat(res.getCashTransactionId()).isNotNull();
        assertThat(res.getBalanceAfter()).isEqualTo(5_000L); // 새 지갑(0원) 생성 후 입금

        // DB 상태 확인
        Payment p = paymentRepository.findById(res.getPaymentId()).orElseThrow();
        assertThat(p.getPaidAt()).isNotNull();

        Cash cash = cashRepository.findByMember(me).orElseThrow();
        assertThat(cash.getBalance()).isEqualTo(5_000L);
    }

    @Test
    void charge_idempotent_같은키면_PG호출없이_이전영수증반환() {
        // 1차 호출 mock
        given(tossBillingClientService.charge(
                ArgumentMatchers.eq("BILL-001"),
                ArgumentMatchers.eq(3_000L),
                ArgumentMatchers.eq("idem-x"),
                ArgumentMatchers.eq("user-" + me.getId())
        )).willReturn(PgChargeResultResponse.builder()
                .success(true)
                .transactionId("PAY-AAA")
                .build());

        // 첫 호출
        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(card.getId());
        req.setAmount(3_000L);
        req.setIdempotencyKey("idem-x");
        PaymentResponse first = paymentService.charge(me, req);

        // 두 번째 호출(같은 멱등키) — tossBillingClientService 가 불리면 안 됨
        PaymentResponse second = paymentService.charge(me, req);

        assertThat(second.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(second.getTransactionId()).isEqualTo("PAY-AAA");
        assertThat(second.getAmount()).isEqualTo(3_000L);

        // PG는 정확히 1번만 호출됨
        verify(tossBillingClientService, times(1))
                .charge("BILL-001", 3_000L, "idem-x", "user-" + me.getId());
    }

    @Test
    void charge_fail_빌링키없으면_400() {
        // 빌링키 없는 수단
        PaymentMethod noBillingKey = paymentMethodRepository.save(
                PaymentMethod.builder()
                        .member(me)
                        .type(PaymentMethodType.CARD)
                        .token(null) // ★ 없음
                        .alias("무효카드")
                        .isDefault(false)
                        .brand("KB")
                        .last4("5555")
                        .expMonth(1)
                        .expYear(2031)
                        .provider("toss")
                        .active(true)
                        .deleted(false)
                        .build()
        );

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(noBillingKey.getId());
        req.setAmount(1_000L);
        req.setIdempotencyKey("idem-bad");

        assertThatThrownBy(() -> paymentService.charge(me, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("billingKey가 없습니다");
    }

    @Test
    void getMyPayments_조회_및_getMyPaymentDetail_조회() {
        // 먼저 하나 성공 결제 만들어두기
        given(tossBillingClientService.charge(
                ArgumentMatchers.eq("BILL-001"),
                ArgumentMatchers.eq(7_000L),
                ArgumentMatchers.eq("idem-list"),
                ArgumentMatchers.eq("user-" + me.getId())
        )).willReturn(PgChargeResultResponse.builder()
                .success(true)
                .transactionId("PAY-LIST")
                .build());

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(card.getId());
        req.setAmount(7_000L);
        req.setIdempotencyKey("idem-list");
        PaymentResponse payRes = paymentService.charge(me, req);

        // 목록 조회
        MyPaymentsResponse list = paymentService.getMyPayments(me, 1, 10);
        assertThat(list.getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(list.getItems()).isNotEmpty();
        assertThat(list.getItems().get(0).getPaymentId()).isEqualTo(payRes.getPaymentId());

        // 단건 상세
        MyPaymentResponse detail = paymentService.getMyPaymentDetail(me, payRes.getPaymentId());
        assertThat(detail.getPaymentId()).isEqualTo(payRes.getPaymentId());
        assertThat(detail.getAmount()).isEqualTo(7_000L);
        assertThat(detail.getTransactionId()).isEqualTo("PAY-LIST");
        assertThat(detail.getPaidAt()).isNotNull();
    }

    @Test
    void charge_fail_PG_4xx면_Service도_그대로_전파() {
        // PG 4xx 흉내
        given(tossBillingClientService.charge(
                ArgumentMatchers.eq("BILL-001"),
                ArgumentMatchers.eq(50_000L),
                ArgumentMatchers.eq("idem-4xx"),
                ArgumentMatchers.eq("user-" + me.getId())
        )).willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Toss 4xx"));

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(card.getId());
        req.setAmount(50_000L);
        req.setIdempotencyKey("idem-4xx");

        assertThatThrownBy(() -> paymentService.charge(me, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Toss 4xx");

        // 실패로 마킹 되었는지(최초 PENDING은 저장됨)
        assertThat(paymentRepository.findAll())
                .anySatisfy(p -> assertThat(p.getStatus().name()).isIn("FAILED", "PENDING"));
    }
}
