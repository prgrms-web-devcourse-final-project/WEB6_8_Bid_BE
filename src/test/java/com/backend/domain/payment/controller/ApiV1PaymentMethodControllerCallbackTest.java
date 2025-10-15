// src/test/java/com/backend/domain/payment/controller/ApiV1PaymentMethodControllerCallbackTest.java
package com.backend.domain.payment.controller;

import com.backend.domain.payment.dto.response.PaymentMethodResponse;
import com.backend.domain.payment.dto.response.TossIssueBillingKeyResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import com.backend.domain.payment.service.TossBillingClientService;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
@TestPropertySource(properties = {
        "app.frontend.base-url=https://www.bid-market.shop"
})
class ApiV1PaymentMethodControllerCallbackTest {

    @Autowired
    MockMvc mvc;

    // 콜백 로직에서 쓰는 서비스들만 Mock
    @MockitoBean TossBillingClientService tossBillingClientService;
    @MockitoBean PaymentMethodService paymentMethodService;

    @Test
    @DisplayName("성공: result=success + 파라미터 정상 → /wallet 으로 302")
    void callback_success() throws Exception {
        var confirm = TossIssueBillingKeyResponse.builder()
                .billingKey("BILL-123")
                .brand("KB")
                .last4("1234")
                .expMonth(12)
                .expYear(2028)
                .build();

        Mockito.when(tossBillingClientService.issueBillingKey(eq("user-123"), eq("AUTH-XYZ")))
                .thenReturn(confirm);
        Mockito.when(paymentMethodService.saveOrUpdateBillingKey(eq(123L), any()))
                .thenReturn(PaymentMethodResponse.builder().id(1L).build());

        mvc.perform(get("/api/v1/paymentMethods/toss/confirm-callback")
                        .param("result", "success")
                        .param("customerKey", "user-123")
                        .param("authKey", "AUTH-XYZ"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.bid-market.shop/wallet"));
    }

    @Test
    @DisplayName("실패: result!=success → /wallet?billing=fail&reason=result_not_success")
    void callback_result_not_success() throws Exception {
        mvc.perform(get("/api/v1/paymentMethods/toss/confirm-callback")
                        .param("result", "fail"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://www.bid-market.shop/wallet?billing=fail&reason=result_not_success"));
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 → /wallet?billing=fail&reason=missing_param")
    void callback_missing_param() throws Exception {
        mvc.perform(get("/api/v1/paymentMethods/toss/confirm-callback")
                        .param("result", "success")
                        .param("customerKey", "user-123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://www.bid-market.shop/wallet?billing=fail&reason=missing_param"));
    }

    @Test
    @DisplayName("실패: PG 4xx/5xx → /wallet?billing=fail&reason=...")
    void callback_pg_error() throws Exception {
        Mockito.when(tossBillingClientService.issueBillingKey(eq("user-123"), eq("BAD")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "PG 4xx: invalid"));

        mvc.perform(get("/api/v1/paymentMethods/toss/confirm-callback")
                        .param("result", "success")
                        .param("customerKey", "user-123")
                        .param("authKey", "BAD"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith("https://www.bid-market.shop/wallet?billing=fail&reason=")));
    }
}
