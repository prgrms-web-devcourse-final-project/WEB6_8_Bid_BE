package com.backend.domain.payment.controller;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.dto.request.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.request.PaymentMethodEditRequest;
import com.backend.domain.payment.dto.response.PaymentMethodResponse;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.service.PaymentMethodService;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
import com.backend.global.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
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
    PaymentMethodRepository paymentMethodRepository;

    @Autowired
    JwtUtil jwtUtil;

    private String bearer(String email) {
        return "Bearer " + jwtUtil.generateAccessToken(email);
    }

    @BeforeEach
    void setUp() {
        paymentMethodRepository.deleteAll();

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
        req.setProvider("toss");

        mvc.perform(post("/api/v1/paymentMethods")
                        .header("Authorization", bearer("user1@example.com"))
                        .with(csrf()) // CSRF 사용 중이면 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("CARD"))
                .andExpect(jsonPath("$.data.alias").value("내 주력카드"));
    }

    @Test
    @DisplayName("결제 등록 다건 조회")
    void list() throws Exception {
        mvc.perform(get("/api/v1/paymentMethods")
                        .header("Authorization", bearer("user1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
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
        req.setProvider("toss");

        PaymentMethodResponse saved = paymentMethodService.create(memberId, req);
        Long id = saved.getId();
        assertThat(id).isNotNull();

        // when & then
        mvc.perform(get("/api/v1/paymentMethods/{id}", id)
                        .header("Authorization", bearer("user1@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.type").value("CARD"))
                .andExpect(jsonPath("$.data.alias").value("경조사용 카드"))
                .andExpect(jsonPath("$.data.last4").value("7777"));
    }

    @Test
    @DisplayName("결제 수단 단건 조회(미존재/타회원) → 404")
    void getOne_notFound() throws Exception {
        mvc.perform(get("/api/v1/paymentMethods/{id}", 99999L)
                        .header("Authorization", bearer("user1@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT(CARD): 카드 필드만 수정 성공 (교차타입 미포함)")
    void edit_card_success() throws Exception {
        // given: 카드 하나 만들고 id 확보
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();
        PaymentMethodCreateRequest c = new PaymentMethodCreateRequest();
        c.setType("CARD"); c.setAlias("수정대상"); c.setIsDefault(false);
        c.setBrand("KB"); c.setLast4("1111"); c.setExpMonth(1); c.setExpYear(2030);
        c.setProvider("toss");
        PaymentMethodResponse saved = paymentMethodService.create(memberId, c);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("경조/여행 전용");
        req.setIsDefault(true);
        req.setBrand("SHINHAN");
        req.setLast4("2222");
        req.setExpMonth(5);
        req.setExpYear(2035);

        mvc.perform(put("/api/v1/paymentMethods/{id}", saved.getId())
                        .header("Authorization", bearer("user1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.type").value("CARD"))
                .andExpect(jsonPath("$.data.alias").value("경조/여행 전용"))
                .andExpect(jsonPath("$.data.brand").value("SHINHAN"))
                .andExpect(jsonPath("$.data.last4").value("2222"))
                .andExpect(jsonPath("$.data.expMonth").value(5))
                .andExpect(jsonPath("$.data.expYear").value(2035))
                // 반대 타입 필드는 null 유지
                .andExpect(jsonPath("$.data.bankName").doesNotExist())
                .andExpect(jsonPath("$.data.bankCode").doesNotExist())
                .andExpect(jsonPath("$.data.acctLast4").doesNotExist());
    }

    @Test
    @DisplayName("PUT(CARD): BANK 필드가 포함되면 400")
    void edit_card_with_bank_fields_badRequest() throws Exception {
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();
        PaymentMethodCreateRequest c = new PaymentMethodCreateRequest();
        c.setType("CARD"); c.setAlias("수정대상"); c.setIsDefault(false);
        c.setBrand("KB"); c.setLast4("1111"); c.setExpMonth(1); c.setExpYear(2030);
        c.setProvider("toss");
        PaymentMethodResponse saved = paymentMethodService.create(memberId, c);

        // 교차 타입 필드 포함
        String body = """
            {
              "alias": "변경시도",
              "bankName": "KB국민은행"
            }
            """;

        mvc.perform(put("/api/v1/paymentMethods/{id}", saved.getId())
                        .header("Authorization", bearer("user1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT(BANK): 은행 필드만 수정 성공")
    void edit_bank_success() throws Exception {
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();
        // given: BANK 생성
        PaymentMethodCreateRequest b = new PaymentMethodCreateRequest();
        b.setType("BANK"); b.setAlias("급여통장"); b.setIsDefault(false);
        b.setBankCode("004"); b.setBankName("KB국민은행"); b.setAcctLast4("5678");
        b.setProvider("toss");
        PaymentMethodResponse saved = paymentMethodService.create(memberId, b);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("월급통장");
        req.setIsDefault(false);
        req.setBankCode("088");
        req.setBankName("신한");
        req.setAcctLast4("9999");

        mvc.perform(put("/api/v1/paymentMethods/{id}", saved.getId())
                        .header("Authorization", bearer("user1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.type").value("BANK"))
                .andExpect(jsonPath("$.data.alias").value("월급통장"))
                .andExpect(jsonPath("$.data.bankCode").value("088"))
                .andExpect(jsonPath("$.data.bankName").value("신한"))
                .andExpect(jsonPath("$.data.acctLast4").value("9999"))
                // CARD 필드는 null 유지
                .andExpect(jsonPath("$.data.brand").doesNotExist())
                .andExpect(jsonPath("$.data.last4").doesNotExist())
                .andExpect(jsonPath("$.data.expMonth").doesNotExist())
                .andExpect(jsonPath("$.data.expYear").doesNotExist());
    }

    @Test
    @DisplayName("PUT: 인증 없이 요청하면 401")
    void edit_unauthorized() throws Exception {
        mvc.perform(put("/api/v1/paymentMethods/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT: 존재하지 않는 결제수단이면 404")
    void edit_notFound() throws Exception {
        // given: 토큰만 유효
        mvc.perform(put("/api/v1/paymentMethods/{id}", 999999L)
                        .header("Authorization", bearer("user1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE: 기본이 아닌 결제수단 삭제 → 200 + {deleted:true, wasDefault:false, newDefaultId:null}")
    void delete_nonDefault_success() throws Exception {
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();

        // 기본 수단 A(기본)
        PaymentMethodCreateRequest a = new PaymentMethodCreateRequest();
        a.setType("CARD"); a.setAlias("A"); a.setIsDefault(true);
        a.setBrand("VISA"); a.setLast4("1111"); a.setExpMonth(12); a.setExpYear(2030);
        a.setProvider("toss");
        paymentMethodService.create(memberId, a);

        // 비기본 수단 B(삭제 대상)
        PaymentMethodCreateRequest b = new PaymentMethodCreateRequest();
        b.setType("BANK"); b.setAlias("B"); b.setIsDefault(false);
        b.setBankName("KB"); b.setAcctLast4("2222"); b.setBankCode("004");
        b.setProvider("toss");
        Long deleteId = paymentMethodService.create(memberId, b).getId();

        mvc.perform(delete("/api/v1/paymentMethods/{id}", deleteId)
                        .header("Authorization", bearer("user1@example.com"))
                        .with(csrf())) // CSRF 사용 중이면 유지
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(deleteId))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.wasDefault").value(false))
                .andExpect(jsonPath("$.data.newDefaultId").value(nullValue()));
    }

    @Test
    @DisplayName("DELETE: 기본 결제수단 삭제 → 최근 생성 수단으로 승계(newDefaultId 세팅)")
    void delete_default_withSuccessor_success() throws Exception {
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();

        // 기본 수단 A(삭제 대상)
        PaymentMethodCreateRequest a = new PaymentMethodCreateRequest();
        a.setType("CARD"); a.setAlias("A"); a.setIsDefault(true);
        a.setBrand("VISA"); a.setLast4("9999"); a.setExpMonth(10); a.setExpYear(2031);
        a.setProvider("toss");
        Long deleteId = paymentMethodService.create(memberId, a).getId();

        // 후속 수단 B(최근 생성 → 승계 대상)
        PaymentMethodCreateRequest b = new PaymentMethodCreateRequest();
        b.setType("BANK"); b.setAlias("B"); b.setIsDefault(false);
        b.setBankName("KB"); b.setAcctLast4("3333"); b.setBankCode("004");
        b.setProvider("toss");
        Long successorId = paymentMethodService.create(memberId, b).getId();

        mvc.perform(delete("/api/v1/paymentMethods/{id}", deleteId)
                        .header("Authorization", bearer("user1@example.com"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(deleteId))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.wasDefault").value(true))
                .andExpect(jsonPath("$.data.newDefaultId").value(successorId));

        // 옵션: 승계된 수단이 정말 기본인지 확인
        PaymentMethodResponse successor = paymentMethodService.findOne(memberId, successorId);
        assertThat(successor.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("DELETE: 기본 결제수단 삭제(마지막 1개) → newDefaultId=null")
    void delete_default_withoutSuccessor_success() throws Exception {
        Long memberId = memberRepository.findByEmail("user1@example.com").orElseThrow().getId();

        PaymentMethodCreateRequest a = new PaymentMethodCreateRequest();
        a.setType("CARD"); a.setAlias("A"); a.setIsDefault(true);
        a.setBrand("VISA"); a.setLast4("4444"); a.setExpMonth(9); a.setExpYear(2032);
        a.setProvider("toss");
        Long deleteId = paymentMethodService.create(memberId, a).getId();

        mvc.perform(delete("/api/v1/paymentMethods/{id}", deleteId)
                        .header("Authorization", bearer("user1@example.com"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(deleteId))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.wasDefault").value(true))
                .andExpect(jsonPath("$.data.newDefaultId").value(nullValue()));
    }
}