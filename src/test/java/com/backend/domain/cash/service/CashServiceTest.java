package com.backend.domain.cash.service;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.dto.response.CashResponse;
import com.backend.domain.cash.dto.response.CashTransactionResponse;
import com.backend.domain.cash.dto.response.CashTransactionsResponse;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.enums.CashTxType;
import com.backend.domain.cash.enums.RelatedType;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.cash.repository.CashTransactionRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.StandardProduct;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CashServiceTest {

    @Autowired
    CashService cashService;

    @Autowired
    CashRepository cashRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CashTransactionRepository cashTransactionRepository;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    EntityManager em;


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

    @Test
    void 내_지갑_조회_성공() {
        Member me = memberRepository.save(
                Member.builder().email("me3@test.com").password("pw").nickname("me3").build()
        );
        Cash myCash = cashRepository.save(Cash.builder().member(me).balance(10_000L).build());

        CashResponse res = cashService.getMyCashResponse(me);

        assertThat(res.getCashId()).isEqualTo(myCash.getId());
        assertThat(res.getMemberId()).isEqualTo(me.getId());
        assertThat(res.getBalance()).isEqualTo(10_000L);
        assertThat(res.getCreateDate()).isNotNull();
        assertThat(res.getModifyDate()).isNotNull();
    }

    @Test
    void 지갑_없으면_404() {
        Member other = memberRepository.save(
                Member.builder().email("no-wallet@test.com").password("pw").build()
        );

        assertThatThrownBy(() -> cashService.getMyCashResponse(other))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("지갑이 아직 생성되지 않았습니다.");
    }

    @Test
    void 내_원장_목록_정렬_및_페이징() {
        // 회원 + 지갑
        Member me = memberRepository.save(
                Member.builder().email("me4@test.com").password("pw").nickname("me4").build()
        );
        Cash myCash = cashRepository.save(Cash.builder().member(me).balance(10_000L).build());

        // 입금 → 출금 (id DESC면 출금이 먼저 나와야 함)
        CashTransaction txDeposit = cashTransactionRepository.save(CashTransaction.builder()
                .cash(myCash)
                .type(CashTxType.DEPOSIT)
                .amount(10_000L)
                .balanceAfter(10_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(1001L)
                .build());

        CashTransaction txWithdraw = cashTransactionRepository.save(CashTransaction.builder()
                .cash(myCash)
                .type(CashTxType.WITHDRAW)
                .amount(3_000L)
                .balanceAfter(7_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(1002L)
                .build());

        CashTransactionsResponse res = cashService.getMyTransactions(me, 1, 20);

        assertThat(res.getPage()).isEqualTo(1);
        assertThat(res.getSize()).isEqualTo(20);
        assertThat(res.getTotal()).isEqualTo(2);
        assertThat(res.getItems()).hasSize(2);

        // 0: 출금
        assertThat(res.getItems().get(0).getType()).isEqualTo("WITHDRAW");
        assertThat(res.getItems().get(0).getAmount()).isEqualTo(3_000L);
        assertThat(res.getItems().get(0).getBalanceAfter()).isEqualTo(7_000L);
        assertThat(res.getItems().get(0).getRelated().getType()).isEqualTo("PAYMENT");
        assertThat(res.getItems().get(0).getRelated().getId()).isEqualTo(1002L);

        // 1: 입금
        assertThat(res.getItems().get(1).getType()).isEqualTo("DEPOSIT");
        assertThat(res.getItems().get(1).getAmount()).isEqualTo(10_000L);
        assertThat(res.getItems().get(1).getBalanceAfter()).isEqualTo(10_000L);
        assertThat(res.getItems().get(1).getRelated().getType()).isEqualTo("PAYMENT");
        assertThat(res.getItems().get(1).getRelated().getId()).isEqualTo(1001L);
    }

    @Test
    void 원장_단건_상세_Payment_링크_생성() {
        // 회원 + 지갑
        Member me = memberRepository.save(
                Member.builder().email("me5@test.com").password("pw").nickname("me5").build()
        );
        Cash myCash = cashRepository.save(Cash.builder().member(me).balance(10_000L).build());

        // 출금 트랜잭션 (PAYMENT)
        CashTransaction txWithdraw = cashTransactionRepository.save(CashTransaction.builder()
                .cash(myCash)
                .type(CashTxType.WITHDRAW)
                .amount(3_000L)
                .balanceAfter(7_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(2002L)
                .build());

        CashTransactionResponse res = cashService.getMyTransactionDetail(me, txWithdraw.getId());

        assertThat(res.getTransactionId()).isEqualTo(txWithdraw.getId());
        assertThat(res.getCashId()).isEqualTo(myCash.getId());
        assertThat(res.getType()).isEqualTo("WITHDRAW");
        assertThat(res.getAmount()).isEqualTo(3_000L);
        assertThat(res.getBalanceAfter()).isEqualTo(7_000L);
        assertThat(res.getCreatedAt()).isNotNull();
        assertThat(res.getRelated().getType()).isEqualTo("PAYMENT");
        assertThat(res.getRelated().getId()).isEqualTo(2002L);
        assertThat(res.getRelated().getLinks().getPaymentDetail())
                .isEqualTo("/api/v1/payments/me/2002");
    }

    @Test
    void 원장_단건_상세_Bid_링크_생성() {
        // 회원 + 지갑
        Member me = memberRepository.save(
                Member.builder().email("me6@test.com").password("pw").nickname("me6").build()
        );
        Cash myCash = cashRepository.save(Cash.builder().member(me).balance(10_000L).build());

        // 상품 생성(생성자 정책에 맞춰 값 채움)
        Product product = new StandardProduct(
                "테스트상품2", "desc",
                ProductCategory.values()[0],
                1_000L,
                LocalDateTime.now().minusHours(1),
                1,
                DeliveryMethod.values()[0],
                "서울",
                me
        );
        em.persist(product);

        // BID 생성
        Bid bid = bidRepository.save(Bid.builder()
                .product(product)
                .member(me)
                .bidPrice(5_000L)
                .status(BidStatus.BIDDING)
                .build());

        // BID 타입 원장 생성
        CashTransaction txBid = cashTransactionRepository.save(CashTransaction.builder()
                .cash(myCash)
                .type(CashTxType.WITHDRAW)
                .amount(5_000L)
                .balanceAfter(5_000L)
                .relatedType(RelatedType.BID)
                .relatedId(bid.getId())
                .build());

        // 상세 조회 → bidDetail 링크 확인
        CashTransactionResponse res = cashService.getMyTransactionDetail(me, txBid.getId());
        assertThat(res.getRelated().getType()).isEqualTo("BID");
        assertThat(res.getRelated().getId()).isEqualTo(bid.getId());
        assertThat(res.getRelated().getLinks().getBidDetail())
                .isEqualTo("/api/v1/bids/products/" + product.getId());
    }

    @Test
    void 타인_원장_상세_404() {
        // 내 계정
        Member me = memberRepository.save(
                Member.builder().email("me7@test.com").password("pw").nickname("me7").build()
        );
        cashRepository.save(Cash.builder().member(me).balance(1_000L).build());

        // 다른 사람 + 지갑 + 트랜잭션
        Member other = memberRepository.save(
                Member.builder().email("other@test.com").password("pw").nickname("other").build()
        );
        Cash otherCash = cashRepository.save(Cash.builder().member(other).balance(1_000L).build());
        CashTransaction othersTx = cashTransactionRepository.save(CashTransaction.builder()
                .cash(otherCash)
                .type(CashTxType.DEPOSIT)
                .amount(1_000L)
                .balanceAfter(1_000L)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(9L)
                .build());

        assertThatThrownBy(() -> cashService.getMyTransactionDetail(me, othersTx.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("원장 내역을 찾을 수 없습니다.");
    }
}
