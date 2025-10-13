package com.backend.domain.payment.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "pg.toss.secretKey=sk_test_xxx"
})
class TossBillingClientServiceTest {

    @Autowired
    TossBillingClientService sut;

    @Autowired
    WireMockServer wireMockServer;

    @BeforeEach
    void overrideWebClient() {
        String base = "http://localhost:" + wireMockServer.port() + "/v1";
        WebClient testWeb = WebClient.builder().baseUrl(base).build();
        ReflectionTestUtils.setField(sut, "web", testWeb);
    }

    // 토스 빌링키 발급 성공 시 응답에서 카드 정보(브랜드/만료/번호)를 올바로 파싱..
    @Test
    void issueBillingKey_success_parses_fields() {
        stubFor(post(urlPathEqualTo("/v1/billing/authorizations/AUTH-123"))
                .willReturn(okJson("""
                  {
                    "billingKey":"BILL-123",
                    "card": {
                      "number":"****-****-****-1234",
                      "company":"SHINHAN",
                      "expireMonth":"12",
                      "expireYear":2030
                    }
                  }
                """)));

        var res = sut.issueBillingKey("user-1","AUTH-123");
        assertThat(res.getBillingKey()).isEqualTo("BILL-123");
        assertThat(res.getCardBrand()).isEqualTo("SHINHAN");
        assertThat(res.getLast4()).isEqualTo("****-****-****-1234");
        assertThat(res.getExpMonth()).isEqualTo(12);
        assertThat(res.getExpYear()).isEqualTo(2030);
    }

    // 빌링키 결제 성공 시 paymentKey를 받아 성공 응답과 거래ID를 반환..
    @Test
    void charge_success_returns_txId() {
        stubFor(post(urlPathEqualTo("/v1/billing/BILL-123"))
                .withHeader("Authorization", matching("Basic .*"))
                .withHeader("Idempotency-Key", equalTo("idem-1"))
                .willReturn(okJson("{\"paymentKey\":\"pay_abc\"}")));

        var res = sut.charge("BILL-123", 5000L, "idem-1", "user-1");
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getTransactionId()).isEqualTo("pay_abc");
    }

    // 결제 응답에 paymentKey가 없으면 502(BAD_GATEWAY) 예외를 던짐..
    @Test
    void charge_missing_paymentKey_throws_502() {
        stubFor(post(urlPathEqualTo("/v1/billing/BILL-123"))
                .willReturn(okJson("{\"status\":\"DONE\"}")));

        assertThatThrownBy(() -> sut.charge("BILL-123", 1000L, "idem-x","user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    // 토스에서 4xx 에러가 오면 서비스도 400(BAD_REQUEST)로 매핑해 예외를 던짐..
    @Test
    void charge_4xx_maps_to_400() {
        stubFor(post(urlPathEqualTo("/v1/billing/BILL-400"))
                .willReturn(aResponse().withStatus(400).withBody("bad req")));

        assertThatThrownBy(() -> sut.charge("BILL-400", 1,"i","user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
