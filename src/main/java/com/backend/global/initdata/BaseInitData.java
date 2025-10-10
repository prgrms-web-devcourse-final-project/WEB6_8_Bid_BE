package com.backend.global.initdata;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import com.backend.domain.cash.enums.CashTxType;
import com.backend.domain.cash.enums.RelatedType;
import com.backend.domain.cash.repository.CashRepository;
import com.backend.domain.cash.repository.CashTransactionRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.domain.payment.enums.PaymentStatus;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.repository.PaymentRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.domain.product.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;
    private final BidRepository bidRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductSyncService productSyncService;
    private final CashRepository cashRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
            productSyncService.reindexAllProducts();
        };
    }

    @Transactional
    public void work1() {
        // 이미 데이터가 있으면 건너뛰기
        if (memberRepository.count() > 0) {
            return;
        }

        // 모든 테스트 데이터 삭제
        bidRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 데이터 생성
        Member bidder1 = memberRepository.save(Member.builder()
                .email("bidder1@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("입찰자1")
                .build());

        Member bidder2 = memberRepository.save(Member.builder()
                .email("bidder2@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("입찰자2")
                .build());

        Member seller = memberRepository.save(Member.builder()
                .email("seller@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("판매자")
                .build());

        Product product1 = productRepository.save(new Product(
                "iPhone 15 Pro",
                "최신 iPhone 15 Pro 새상품입니다.",
                com.backend.domain.product.enums.ProductCategory.DIGITAL_ELECTRONICS,
                1000000L,
                java.time.LocalDateTime.now().minusHours(1), // 1시간 전 시작
                24, // 24시간 경매
                com.backend.domain.product.enums.DeliveryMethod.DELIVERY,
                "서울시 강남구",
                seller
        ));

        Product product2 = productRepository.save(new Product(
                "MacBook Pro M3",
                "MacBook Pro 14인치 M3 칩셋 모델입니다.",
                com.backend.domain.product.enums.ProductCategory.DIGITAL_ELECTRONICS,
                2000000L,
                java.time.LocalDateTime.now().minusMinutes(30), // 30분 전 시작
                48, // 48시간 경매
                com.backend.domain.product.enums.DeliveryMethod.DELIVERY,
                "서울시 서초구",
                seller
        ));

        Product product3 = productRepository.save(new Product(
                "AirPods Pro 2세대",
                "Apple AirPods Pro 2세대 노이즈캔슬링 이어폰입니다.",
                com.backend.domain.product.enums.ProductCategory.DIGITAL_ELECTRONICS,
                200000L,
                java.time.LocalDateTime.now().minusMinutes(15), // 15분 전 시작
                12, // 12시간 경매
                com.backend.domain.product.enums.DeliveryMethod.TRADE,
                "서울시 홍대",
                seller
        ));

        product1.setCurrentPrice(1200000L);
        product2.setCurrentPrice(2100000L);
        product3.setCurrentPrice(220000L);
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        bidRepository.save(Bid.builder()
                .bidPrice(1200000L)
                .product(product1)
                .member(bidder1)
                .status(BidStatus.BIDDING)
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(1100000L)
                .product(product1)
                .member(bidder2)
                .status(BidStatus.BIDDING)
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(2100000L)
                .product(product2)
                .member(bidder2)
                .status(BidStatus.BIDDING)
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(220000L)
                .product(product3)
                .member(bidder1)
                .status(BidStatus.BIDDING)
                .build());

        Cash bidder1Cash = cashRepository.findByMember(bidder1)
                .orElseGet(() -> cashRepository.save(
                        Cash.builder()
                                .member(bidder1)
                                .balance(0L)
                                .build()
                ));

        Cash bidder2Cash = cashRepository.findByMember(bidder2)
                .orElseGet(() -> cashRepository.save(
                        Cash.builder()
                                .member(bidder2)
                                .balance(0L)
                                .build()
                ));

        // 충전: 입금(+150,000)..
        long b1After = bidder1Cash.getBalance() + 150_000L;
        bidder1Cash.setBalance(b1After);
        cashRepository.save(bidder1Cash);

        CashTransaction b1Deposit = CashTransaction.builder()
                .cash(bidder1Cash)
                .type(CashTxType.DEPOSIT)
                .amount(150_000L)           // 입금은 양수..
                .balanceAfter(b1After)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(101L)            // 샘플 결제ID(일단 아무 값이나 고정)..
                .build();
        cashTransactionRepository.save(b1Deposit);

        // 충전: 입금(+80,000)..
        long b2After = bidder2Cash.getBalance() + 80_000L;
        bidder2Cash.setBalance(b2After);
        cashRepository.save(bidder2Cash);

        CashTransaction b2Deposit = CashTransaction.builder()
                .cash(bidder2Cash)
                .type(CashTxType.DEPOSIT)
                .amount(80_000L)
                .balanceAfter(b2After)
                .relatedType(RelatedType.PAYMENT)
                .relatedId(102L)
                .build();
        cashTransactionRepository.save(b2Deposit);

        // 출금: 낙찰 결제(-32,000) — 음수로 저장..
        long pay = 32_000L;
        if (bidder1Cash.getBalance() >= pay) {
            long afterW = bidder1Cash.getBalance() - pay;
            bidder1Cash.setBalance(afterW);
            cashRepository.save(bidder1Cash);

            CashTransaction b1Withdraw = CashTransaction.builder()
                    .cash(bidder1Cash)
                    .type(CashTxType.WITHDRAW)
                    .amount(-pay)                   // 출금은 음수
                    .balanceAfter(afterW)
                    .relatedType(RelatedType.BID)
                    .relatedId(3456L)               // 샘플 입찰ID
                    .build();
            cashTransactionRepository.save(b1Withdraw);
        }

        // 카드 등록
        PaymentMethod b1Card = paymentMethodRepository.save(
                PaymentMethod.builder()
                        .member(bidder1)
                        .methodType(PaymentMethodType.CARD)
                        .alias("주거래 카드")
                        .brand("KB")
                        .last4("4321")
                        .expMonth(3)
                        .expYear(2028)
                        .isDefault(true)
                        .provider("toss")
                        .build()
        );

        // 계좌 등록
        PaymentMethod b1Bank = paymentMethodRepository.save(
                PaymentMethod.builder()
                        .member(bidder1)
                        .methodType(PaymentMethodType.BANK)
                        .alias("급여통장")
                        .bankCode("004")
                        .bankName("KB국민은행")
                        .acctLast4("5678")
                        .isDefault(false)
                        .provider("toss")
                        .build()
        );

        // 결제(충전)..
        long charge = 50_000L;
        long after = bidder1Cash.getBalance() + charge;
        bidder1Cash.setBalance(after);
        cashRepository.save(bidder1Cash);

        CashTransaction cTx = cashTransactionRepository.save(
                CashTransaction.builder()
                        .cash(bidder1Cash)
                        .type(CashTxType.DEPOSIT)
                        .amount(charge)              // 입금은 양수
                        .balanceAfter(after)
                        .relatedType(RelatedType.PAYMENT)
                        .relatedId(3456L)
                        .build()
        );

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .member(bidder1)
                        .paymentMethod(b1Card)
                        .status(PaymentStatus.SUCCESS)
                        .amount(charge)
                        .provider("toss")
                        .methodType(PaymentMethodType.CARD)
                        .transactionId("pg_tx_seed_001")
                        .idempotencyKey("seed-" + System.currentTimeMillis())
                        .cashTransaction(cTx)
                        .build()
        );

    }
}
