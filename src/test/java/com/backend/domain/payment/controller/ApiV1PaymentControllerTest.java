package com.backend.domain.payment.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.domain.payment.dto.request.PaymentRequest;
import com.backend.domain.payment.dto.response.PgChargeResultResponse;
import com.backend.domain.payment.dto.response.TossIssueBillingKeyResponse;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.repository.PaymentRepository;
import com.backend.domain.payment.service.TossBillingClientService;
import com.backend.global.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @Autowired MemberRepository memberRepository;
    @Autowired PaymentMethodRepository paymentMethodRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired
    JwtUtil jwtUtil;
    private String bearer(String email) {
        return "Bearer " + jwtUtil.generateAccessToken(email);
    }
    // 외부 호출은 Mock 처리 (실제 네트워크 X)
    @MockitoBean
    TossBillingClientService tossBillingClientService;

    private String email;           // @WithMockUser(username=...) 값과 같아야 함
    private Member me;
    private PaymentMethod pmCard;   // 카드 결제수단
    private String billingKey;

    @BeforeEach
    void setUp() {
        email = "pay@test.com";
        billingKey = "billingKey-123";

        // 회원 저장 (password NOT NULL 이므로 꼭 채워야 함)
        me = memberRepository.save(
                Member.builder()
                        .email(email)
                        .password("pw!")
                        .nickname("payer")
                        .build()
        );

        // 결제수단 저장 (CARD, token=billingKey)
        pmCard = paymentMethodRepository.save(
                PaymentMethod.builder()
                        .member(me)
                        .methodType(PaymentMethodType.CARD)
                        .token(billingKey)   // 토스 빌링키
                        .alias("메인카드")
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

    // 1) 지갑 충전 성공
    @Test
    @WithMockUser(username = "pay@test.com") // 컨트롤러가 user.getUsername()으로 email을 읽음
    void charge_success() throws Exception {
        // toss 결제 Mock: 성공 응답
        given(tossBillingClientService.charge(eq(billingKey), eq(5_000L), anyString(), anyString()))
                .willReturn(PgChargeResultResponse.builder()
                        .success(true)
                        .transactionId("payKey_abc123")
                        .build());

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(pmCard.getId());
        req.setAmount(5_000L);
        req.setIdempotencyKey("idem-001");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                // PaymentResponse(JSON) 필드 검증
                .andExpect(jsonPath("$.data.paymentId", notNullValue()))
                .andExpect(jsonPath("$.data.paymentMethodId", is(pmCard.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.amount", is(5_000)))
                .andExpect(jsonPath("$.data.currency", is("KRW")))
                .andExpect(jsonPath("$.data.provider", is("toss")))
                .andExpect(jsonPath("$.data.methodType", is("CARD")))
                .andExpect(jsonPath("$.data.transactionId", is("payKey_abc123")))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.paidAt", notNullValue()))
                .andExpect(jsonPath("$.data.idempotencyKey", is("idem-001")))
                .andExpect(jsonPath("$.data.balanceAfter", notNullValue()));
    }

    // 2) 멱등: 같은 키로 두 번 쏘면 같은 결과
    @Test
    @WithMockUser(username = "pay@test.com")
    void charge_idempotent_returns_same_result() throws Exception {
        // 첫 호출 성공
        given(tossBillingClientService.charge(eq(billingKey), eq(3_000L), anyString(), anyString()))
                .willReturn(PgChargeResultResponse.builder()
                        .success(true)
                        .transactionId("payKey_idem")
                        .build());

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(pmCard.getId());
        req.setAmount(3_000L);
        req.setIdempotencyKey("idem-dup");

        // 1차 호출
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.transactionId", is("payKey_idem")));

        // 2차 호출(같은 키) → PG 안 타고 기존 결과 그대로
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.transactionId", is("payKey_idem")));
    }

    // 3) 내 결제 목록 조회
    @Test
    @WithMockUser(username = "pay@test.com")
    void get_my_payments_list() throws Exception {
        // 하나 만들어두기
        given(tossBillingClientService.charge(eq(billingKey), anyLong(), anyString(), anyString()))
                .willReturn(PgChargeResultResponse.builder().success(true).transactionId("k1").build());

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(pmCard.getId());
        req.setAmount(2_500L);
        req.setIdempotencyKey("idem-list-1");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments/me")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page", is(1)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].paymentId", notNullValue()))
                .andExpect(jsonPath("$.data.items[0].amount", notNullValue()))
                .andExpect(jsonPath("$.data.items[0].status", notNullValue()));
    }

    // 4) 내 결제 단건 상세
    @Test
    @WithMockUser(username = "pay@test.com")
    void get_my_payment_detail() throws Exception {
        // 하나 충전하고 그 id로 상세 조회
        given(tossBillingClientService.charge(eq(billingKey), eq(4_000L), anyString(), anyString()))
                .willReturn(PgChargeResultResponse.builder().success(true).transactionId("k2").build());

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(pmCard.getId());
        req.setAmount(4_000L);
        req.setIdempotencyKey("idem-detail");

        // 생성 응답에서 paymentId 추출
        String body = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var root = om.readTree(body);
        // JsonPath로 꺼내도 되고, 간단히 Jackson으로 map 해도 됨
        long paymentId = root.path("data").path("paymentId").asLong(root.path("paymentId").asLong(0));
        assertTrue(paymentId > 0, "paymentId must be positive");

        mockMvc.perform(get("/api/v1/payments/me/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId", is((int) paymentId)))
                .andExpect(jsonPath("$.data.amount", is(4_000)))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.transactionId", is("k2")))
                .andExpect(jsonPath("$.data.paidAt", notNullValue()));
    }

    // 5) 토스 빌링키 발급 API
    @Test
    @WithMockUser(username = "pay@test.com")
    void issue_billing_key_success() throws Exception {
        given(tossBillingClientService.issueBillingKey(org.mockito.ArgumentMatchers.startsWith("user-"), eq("AUTH-123")))
                .willReturn(TossIssueBillingKeyResponse.builder()
                        .billingKey("BILL-XYZ")
                        .provider("toss")
                        .brand("SHINHAN")
                        .last4("****-****-****-1234")
                        .expMonth(12)
                        .expYear(2030)
                        .build());

        String payload = """
                {"authKey":"AUTH-123"}
                """;

        mockMvc.perform(post("/api/v1/payments/toss/issue-billing-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingKey", is("BILL-XYZ")))
                .andExpect(jsonPath("$.data.provider", is("toss")))
                .andExpect(jsonPath("$.data.cardBrand", is("SHINHAN")));
    }

    // 6) 인증 없으면 401
    @Test
    void charge_unauthorized_401() throws Exception {
        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethodId(999L);
        req.setAmount(1000L);
        req.setIdempotencyKey("idem-unauth");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // 토스 카드등록(빌링) 팝업 파라미터 조회
    @Test
    @WithMockUser(username = "pay@test.com")
    void get_billing_auth_params_success() throws Exception {
        // setUp()에서 me 저장되어 있음: customerKey = "user-" + me.getId()
        String expectedCustomerKey = "user-" + me.getId();

        mockMvc.perform(get("/api/v1/payments/toss/billing-auth-params"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientKey", notNullValue()))                          // 값만 존재 검증 (테스트 환경 clientKey 미설정 대비)
                .andExpect(jsonPath("$.data.customerKey", is(expectedCustomerKey)))              // user-{id}
                .andExpect(jsonPath("$.data.successUrl", containsString("/payments/toss/billing-success.html")))
                .andExpect(jsonPath("$.data.failUrl", containsString("/payments/toss/billing-fail.html")));
    }

    // 멱등키 발급
    @Test
    void new_idempotency_key_success() throws Exception {
        mockMvc.perform(get("/api/v1/payments/idempotency-key")
                        .header("Authorization", bearer("pay@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotencyKey", notNullValue()))
                // 간단한 UUID 패턴(36자 16진/하이픈) 체크
                .andExpect(jsonPath("$.data.idempotencyKey", matchesPattern("^[0-9a-fA-F\\-]{36}$")));
    }
}
