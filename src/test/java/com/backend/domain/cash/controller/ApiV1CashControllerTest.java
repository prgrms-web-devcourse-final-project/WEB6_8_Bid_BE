package com.backend.domain.cash.controller;

import com.backend.domain.cash.constant.CashTxType;
import com.backend.domain.cash.constant.RelatedType;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.cash.repository.CashTransactionRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // JWT 등 보안 필터 비활성화
@Transactional
class ApiV1CashControllerTest {

    @Autowired MockMvc mvc;

    @Autowired MemberRepository memberRepository;
    @Autowired CashRepository cashRepository;
    @Autowired CashTransactionRepository cashTxRepository;

    Member me;
    Member other;
    Cash myCash;
    CashTransaction txDeposit;  // id가 더 작음
    CashTransaction txWithdraw; // id가 더 큼 (목록에서 먼저 나와야 함)

    @BeforeEach
    void setUp() {
        // 회원(비번은 NOT NULL이라 꼭 채움)
        me = memberRepository.save(Member.builder()
                .email("me@test.com").password("pw").nickname("me").build());
        other = memberRepository.save(Member.builder()
                .email("other@test.com").password("pw").nickname("other").build());

        // 내 지갑(잔액 10,000)
        myCash = cashRepository.save(Cash.builder().member(me).balance(10_000L).build());

        // 원장 2건: 입금(10,000) → 출금(3,000) (정상 흐름)
        txDeposit = cashTxRepository.save(CashTransaction.builder()
                .cash(myCash)
                .type(CashTxType.DEPOSIT)
                .amount(10_000L)
                .balanceAfter(10_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(101L) // 임의 값
                .build());

        txWithdraw = cashTxRepository.save(CashTransaction.builder()
                .cash(myCash)
                .type(CashTxType.WITHDRAW)
                .amount(3_000L)
                .balanceAfter(7_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(102L) // 임의 값
                .build());

        // other 유저의 지갑/원장(권한 차단 테스트용)
        Cash otherCash = cashRepository.save(Cash.builder().member(other).balance(5_000L).build());
        cashTxRepository.save(CashTransaction.builder()
                .cash(otherCash)
                .type(CashTxType.DEPOSIT)
                .amount(5_000L)
                .balanceAfter(5_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(201L)
                .build());
    }

    // ============ /api/v1/cash ============
    @Test
    @WithMockUser(username = "me@test.com") // 컨트롤러는 email로 회원 조회
    void 내_지갑_조회_200() throws Exception {
        mvc.perform(get("/api/v1/cash"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId", is(me.getId().intValue())))
                .andExpect(jsonPath("$.cashId", is(myCash.getId().intValue())))
                .andExpect(jsonPath("$.balance", is(10_000)))
                .andExpect(jsonPath("$.createDate", notNullValue()))
                .andExpect(jsonPath("$.modifyDate", notNullValue()));
    }

    @Test
    void 내_지갑_조회_인증없으면_401() throws Exception {
        // GlobalExceptionHandler가 ResponseStatusException을 RsData로 변환하진 않으므로
        // 여기서는 상태코드만 검사
        mvc.perform(get("/api/v1/cash"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ============ /api/v1/cash/transactions ============
    @Test
    @WithMockUser(username = "me@test.com")
    void 내_원장_목록_200_정렬검증() throws Exception {
        mvc.perform(get("/api/v1/cash/transactions")
                        .param("page", "1")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.total", is(2)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                // 최신(id DESC) → 출금(7000) 먼저
                .andExpect(jsonPath("$.items[0].type", is("WITHDRAW")))
                .andExpect(jsonPath("$.items[0].amount", is(3_000)))
                .andExpect(jsonPath("$.items[0].balanceAfter", is(7_000)))
                .andExpect(jsonPath("$.items[0].related.type", is("PAYMENT")))
                .andExpect(jsonPath("$.items[0].related.id", is(102)))

                // 그 다음 입금(10000)
                .andExpect(jsonPath("$.items[1].type", is("DEPOSIT")))
                .andExpect(jsonPath("$.items[1].amount", is(10_000)))
                .andExpect(jsonPath("$.items[1].balanceAfter", is(10_000)))
                .andExpect(jsonPath("$.items[1].related.type", is("PAYMENT")))
                .andExpect(jsonPath("$.items[1].related.id", is(101)));
    }

    @Test
    void 내_원장_목록_인증없으면_401() throws Exception {
        mvc.perform(get("/api/v1/cash/transactions"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ============ /api/v1/cash/transactions/{id} ============
    @Test
    @WithMockUser(username = "me@test.com")
    void 내_원장_단건_상세_200_링크존재() throws Exception {
        mvc.perform(get("/api/v1/cash/transactions/{id}", txWithdraw.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(txWithdraw.getId().intValue())))
                .andExpect(jsonPath("$.cashId", is(myCash.getId().intValue())))
                .andExpect(jsonPath("$.type", is("WITHDRAW")))
                .andExpect(jsonPath("$.amount", is(3_000)))
                .andExpect(jsonPath("$.balanceAfter", is(7_000)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                // Related + Links (PAYMENT면 paymentDetail 링크 생성)
                .andExpect(jsonPath("$.related.type", is("PAYMENT")))
                .andExpect(jsonPath("$.related.id", is(102)))
                .andExpect(jsonPath("$.related.links.paymentDetail", is("/api/v1/payments/me/102")));
    }

    @Test
    @WithMockUser(username = "me@test.com")
    void 다른사람_원장_단건_조회시_404() throws Exception {
        // other 유저의 트랜잭션 id를 찾아서 접근 시도
        Long othersTxId = cashTxRepository.findAll().stream()
                .filter(tx -> tx.getCash().getMember().getId().equals(other.getId()))
                .map(CashTransaction::getId)
                .findFirst()
                .orElseThrow();

        // CashService는 ResponseStatusException(404)을 던짐 → 전역 핸들러가 RsData로 바꾸지 않으므로 상태만 체크
        mvc.perform(get("/api/v1/cash/transactions/{id}", othersTxId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
