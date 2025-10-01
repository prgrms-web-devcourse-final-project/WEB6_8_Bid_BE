package com.backend.domain.cash.service;

import com.backend.domain.cash.constant.CashTxType;
import com.backend.domain.cash.constant.RelatedType;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CashServiceTest {

    @Autowired CashService cashService;
    @Autowired CashRepository cashRepository;
    @Autowired MemberRepository memberRepository;

    @Test
    void 출금_성공하면_잔액줄고_WITHDRAW원장생성() {
        // 준비: 회원 + 지갑(잔액 10_000)
        Member me = memberRepository.save(
                Member.builder()
                        .email("me@test.com")
                        .password("pw!")
                        .nickname("me")
                        .build()
        );
        Cash cash = cashRepository.save(Cash.builder().member(me).balance(10_000L).build());

        // 실행: 8_000원 출금
        CashTransaction tx = cashService.withdraw(me, 8_000L, RelatedType.BID, 123L);

        // 검증: 잔액 2_000, WITHDRAW, 금액 8_000, 관련 타입 BID
        assertThat(cash.getBalance()).isEqualTo(2_000L);
        assertThat(tx.getType()).isEqualTo(CashTxType.WITHDRAW);
        assertThat(tx.getAmount()).isEqualTo(8_000L);
        assertThat(tx.getBalanceAfter()).isEqualTo(2_000L);
        assertThat(tx.getRelatedType()).isEqualTo(RelatedType.BID);
        assertThat(tx.getRelatedId()).isEqualTo(123L);
    }

    @Test
    void 잔액부족이면_출금_실패() {
        Member me = memberRepository.save(
                Member.builder()
                        .email("me2@test.com")
                        .password("pw!!")
                        .nickname("med")
                        .build()
        );
        Cash cash = cashRepository.save(Cash.builder().member(me).balance(5_000L).build());

        // 실행+검증: 8_000 출금 시 예외
        assertThatThrownBy(() -> cashService.withdraw(me, 8_000L, RelatedType.BID, 1L))
                .hasMessageContaining("잔액이 부족");
        assertThat(cash.getBalance()).isEqualTo(5_000L);
    }
}
