package com.backend.domain.payment.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.dto.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.PaymentMethodResponse;
import com.backend.domain.payment.service.PaymentMethodService;
import com.backend.global.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1PaymentMethodControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    PaymentMethodService paymentMethodService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    JwtUtil jwtUtil;

    private String bearer(String email) {
        return "Bearer " + jwtUtil.generateAccessToken(email);
    }

    @BeforeEach
    void setUp() {
        memberRepository.findByEmail("user1@example.com")
                .orElseGet(() -> memberRepository.save(
                        Member.builder().email("user1@example.com").build()
                ));
    }

    @Test
    @DisplayName("결제 수단 등록")
    void create() throws Exception {
        PaymentMethodCreateRequest req = new PaymentMethodCreateRequest();
        req.setType("CARD");
        req.setAlias("내 주력카드");
        req.setIsDefault(true);
        req.setBrand("VISA");
        req.setLast4("1234");
        req.setExpMonth(12);
        req.setExpYear(2030);

        mvc.perform(post("/api/v1/paymentMethods")
                        .header("Authorization", bearer("user1@example.com"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()) // CSRF 사용 중이면 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.alias").value("내 주력카드"));
    }

    @Test
    @DisplayName("결제 등록 다건 조회")
    void list() throws Exception {
        mvc.perform(get("/api/v1/paymentMethods")
                        .header("Authorization", bearer("user1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("결제 수단 단건 조회")
    void getOne_success() throws Exception {
        // given: 실제 서비스로 하나 생성해서 id 확보
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();
        PaymentMethodCreateRequest req = new PaymentMethodCreateRequest();
        req.setType("CARD");
        req.setAlias("경조사용 카드");
        req.setIsDefault(true);
        req.setBrand("KB");
        req.setLast4("7777");
        req.setExpMonth(3);
        req.setExpYear(2029);

        PaymentMethodResponse saved = paymentMethodService.create(memberId, req);
        Long id = saved.getId();
        assertThat(id).isNotNull();

        // when & then
        mvc.perform(get("/api/v1/paymentMethods/{id}", id)
                        .header("Authorization", bearer("user1@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.alias").value("경조사용 카드"))
                .andExpect(jsonPath("$.last4").value("7777"));
    }

    @Test
    @DisplayName("결제 수단 단건 조회(미존재/타회원) → 404")
    void getOne_notFound() throws Exception {
        mvc.perform(get("/api/v1/paymentMethods/{id}", 99999L)
                        .header("Authorization", bearer("user1@example.com")))
                .andExpect(status().isNotFound());
    }

}