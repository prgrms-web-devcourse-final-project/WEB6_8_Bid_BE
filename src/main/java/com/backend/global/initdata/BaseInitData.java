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
import com.backend.domain.notification.service.NotificationQueueService;
import com.backend.domain.payment.entity.Payment;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.domain.payment.enums.PaymentStatus;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import com.backend.domain.payment.repository.PaymentRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.entity.StandardProduct;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.jpa.ProductImageRepository;
import com.backend.domain.product.repository.jpa.ProductRepository;
import com.backend.domain.product.service.ProductSyncService;
import com.backend.domain.review.dto.ReviewRequest;
import com.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//@Profile("dev")
@Profile("!test")
@Component
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BidRepository bidRepository;
    private final CashRepository cashRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;
    private final ProductSyncService productSyncService;
    private final NotificationQueueService notificationQueueService;
    private final ReviewService reviewService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
            productSyncService.indexAllProducts();
        };
    }

    @Transactional
    public void work1() {
        // 이미 데이터가 있으면 건너뛰기
//        if (memberRepository.count() > 0) {
//            return;
//        }

        // ========== 1. 회원 생성 (10명) ==========
        List<Member> members = createMembers();

        // ========== 2. 지갑 & 결제수단 생성 ==========
        createWalletsAndPaymentMethods(members);

        // ========== 3. 상품 생성 (카테고리별 다양하게) ==========
        List<Product> products = createProducts(members);

        // ========== 4. 입찰 생성 ==========
        createBids(members, products);

        // ========== 5. 결제 내역 생성 ==========
        createPayments(members);

        // ========== 6. 알림 생성 ==========
        createNotifications(members, products);

        // ========== 7. 리뷰 생성 ==========
        createReviews(products);
    }

    // ========================================
    // 1. 회원 생성
    // ========================================
    private List<Member> createMembers() {
        List<Member> members = new ArrayList<>();

        // 관리자
        members.add(memberRepository.save(Member.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .nickname("관리자")
                .phoneNumber("010-0000-0000")
                .address("서울특별시 강남구")
                .authority("ROLE_ADMIN")
                .creditScore(100)
                .build()));

        // 일반 회원 (판매자/입찰자)
        String[] nicknames = {
                "전자기기왕", "패션러버", "스포츠매니아", "책벌레",
                "명품수집가", "게이머", "음악광", "캠핑러버", "요리사"
        };

        String[] locations = {
                "서울특별시 강남구", "경기도 수원시", "부산광역시 해운대구",
                "대구광역시 중구", "인천광역시 남동구", "서울특별시 마포구",
                "경기도 성남시", "부산광역시 남구", "대전광역시 유성구"
        };

        for (int i = 0; i < nicknames.length; i++) {
            members.add(memberRepository.save(Member.builder()
                    .email("user" + (i+1) + "@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .nickname(nicknames[i])
                    .phoneNumber("010-" + String.format("%04d", 1000 + i) + "-" + String.format("%04d", 1000 + i))
                    .address(locations[i])
                    .authority("ROLE_USER")
                    .creditScore(50 + (i * 5))
                    .build()));
        }

        return members;
    }

    // ========================================
    // 2. 지갑 & 결제수단 생성
    // ========================================
    private void createWalletsAndPaymentMethods(List<Member> members) {
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);

            // 지갑 생성 (초기 잔액 다양하게)
            long initialBalance = (i % 3 == 0) ? 500000L : (i % 3 == 1) ? 1000000L : 200000L;
            Cash cash = cashRepository.save(Cash.builder()
                    .member(member)
                    .balance(initialBalance)
                    .build());

            // 초기 충전 원장
            cashTransactionRepository.save(CashTransaction.builder()
                    .cash(cash)
                    .type(CashTxType.DEPOSIT)
                    .amount(initialBalance)
                    .balanceAfter(initialBalance)
                    .relatedType(RelatedType.PAYMENT)
                    .relatedId((long) (1000 + i))
                    .build());

            // 결제수단 (카드 + 계좌)
            String[] cardBrands = {"신한", "KB국민", "삼성", "현대", "우리", "하나", "NH농협", "롯데"};
            String[] bankNames = {"KB국민은행", "신한은행", "우리은행", "하나은행", "NH농협"};

            // 카드 등록
            paymentMethodRepository.save(PaymentMethod.builder()
                    .member(member)
                    .methodType(PaymentMethodType.CARD)
                    .alias(member.getNickname() + "의 주카드")
                    .brand(cardBrands[i % cardBrands.length])
                    .last4(String.format("%04d", 1000 + i))
                    .expMonth((i % 12) + 1)
                    .expYear(2026 + (i % 3))
                    .isDefault(true)
                    .provider("toss")
                    .token("tok_" + member.getId() + "_card")
                    .build());

            // 계좌 등록 (50% 확률)
            if (i % 2 == 0) {
                paymentMethodRepository.save(PaymentMethod.builder()
                        .member(member)
                        .methodType(PaymentMethodType.BANK)
                        .alias(member.getNickname() + "의 급여통장")
                        .bankCode(String.format("%03d", (i % 5) + 1))
                        .bankName(bankNames[i % bankNames.length])
                        .acctLast4(String.format("%04d", 5000 + i))
                        .isDefault(false)
                        .provider("toss")
                        .token("tok_" + member.getId() + "_bank")
                        .build());
            }
        }
    }

    // ========================================
    // 3. 상품 생성 (카테고리별)
    // ========================================
    private List<Product> createProducts(List<Member> members) {
        List<Product> products = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 디지털/가전 (5개) - 경매 중
        products.add(createProduct(members.get(1), "아이폰 15 Pro 256GB", "미개봉 새제품입니다.",
                ProductCategory.DIGITAL_ELECTRONICS, 1200000L, now.minusHours(5), 24,
                DeliveryMethod.BOTH, "서울특별시 강남구", "https://i.postimg.cc/85pkBWcS/iphone15-1.jpg"));

        products.add(createProduct(members.get(1), "iPhone 15 Pro", "최신 iPhone 15 Pro 새상품입니다.",
                ProductCategory.DIGITAL_ELECTRONICS, 1100000L, now.minusHours(4), 24,
                DeliveryMethod.BOTH, "서울특별시 강남구", "https://i.postimg.cc/pTWVY8T4/iphone15-2.jpg"));

        products.add(createProduct(members.get(1), "갤럭시 S24 Ultra 512GB", "직거래 우선합니다.",
                ProductCategory.DIGITAL_ELECTRONICS, 1400000L, now.minusHours(3), 24,
                DeliveryMethod.TRADE, "서울특별시 강남구", "https://i.postimg.cc/63W6LR3P/galaxy24.jpg"));

        products.add(createProduct(members.get(6), "맥북 Pro M3 14인치", "거의 새것입니다.",
                ProductCategory.DIGITAL_ELECTRONICS, 2200000L, now.minusHours(2), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/C1hMHD16/macbook.jpg"));

        products.add(createProduct(members.get(6), "에어팟 Pro 2세대", "깨끗하게 사용했습니다.",
                ProductCategory.DIGITAL_ELECTRONICS, 280000L, now.minusHours(1), 24,
                DeliveryMethod.BOTH, "경기도 수원시", "https://i.postimg.cc/hGbPdMDR/airpods.jpg"));

        products.add(createProduct(members.get(6), "LG 그램 17인치 노트북", null,
                ProductCategory.DIGITAL_ELECTRONICS, 1800000L, now.minusMinutes(30), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/bvHNtLz4/lggram.jpg"));

        // 패션/의류 (4개) - 경매 중
        products.add(createProduct(members.get(2), "구찌 GG 마몽 숄더백", "정품 보증서 있습니다.",
                ProductCategory.FASHION_CLOTHING, 1500000L, now.minusHours(4), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/wBcTmF6Z/gucci-bag.jpg"));

        products.add(createProduct(members.get(2), "버버리 트렌치코트", "S사이즈, 한번만 착용",
                ProductCategory.FASHION_CLOTHING, 800000L, now.minusHours(2), 24,
                DeliveryMethod.BOTH, "부산광역시 해운대구", "https://i.postimg.cc/Jhc4kK1g/burberry.jpg"));

        products.add(createProduct(members.get(5), "나이키 에어맥스", "270mm, 새신발",
                ProductCategory.FASHION_CLOTHING, 150000L, now.minusHours(1), 24,
                DeliveryMethod.TRADE, "서울특별시 강남구", "https://i.postimg.cc/pdYXFk2s/nike.jpg"));

        products.add(createProduct(members.get(5), "루이비통 모노그램 지갑", null,
                ProductCategory.FASHION_CLOTHING, 600000L, now.minusMinutes(45), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/YSfqW8pb/lv-wallet.jpg"));

        // 스포츠/레저 (3개) - 경매 중
        products.add(createProduct(members.get(3), "캠핑용 텐트 (4인용)", "한번만 사용",
                ProductCategory.SPORTS_LEISURE, 200000L, now.minusHours(3), 24,
                DeliveryMethod.BOTH, "경기도 성남시", "https://i.postimg.cc/q7XM3Qkw/tent.jpg"));

        products.add(createProduct(members.get(3), "자전거 (로드바이크)", "풀세트 판매",
                ProductCategory.SPORTS_LEISURE, 1200000L, now.minusHours(2), 48,
                DeliveryMethod.TRADE, "대전광역시 유성구", "https://i.postimg.cc/cJM4nTsQ/bike.jpg"));

        products.add(createProduct(members.get(8), "골프채 세트", "초보용, 거의 새것",
                ProductCategory.SPORTS_LEISURE, 800000L, now.minusHours(1), 24,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/Jhc4kK1j/golf.jpg"));

        // 도서/음반 (2개) - 경매 중
        products.add(createProduct(members.get(4), "해리포터 전집 (영문판)", "초판 한정판",
                ProductCategory.BOOKS_MEDIA, 150000L, now.minusHours(6), 24,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/ZqFKdjTc/harry-potter.jpg"));

        products.add(createProduct(members.get(7), "비틀즈 LP 박스세트", "희귀 앨범",
                ProductCategory.BOOKS_MEDIA, 500000L, now.minusHours(4), 48,
                DeliveryMethod.TRADE, "부산광역시 남구", "https://i.postimg.cc/mrGgkKGg/beatles.jpg"));

        // 취미/수집품 (2개) - 경매 중
        products.add(createProduct(members.get(4), "레고 밀레니엄 팔콘", "미개봉 한정판",
                ProductCategory.HOBBY_COLLECTIBLES, 1200000L, now.minusHours(5), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/wBh3kgk3/lego.jpg"));

        products.add(createProduct(members.get(7), "포켓몬 카드 (초판)", "희귀 카드 50장",
                ProductCategory.HOBBY_COLLECTIBLES, 800000L, now.minusHours(2), 24,
                DeliveryMethod.BOTH, "인천광역시 남동구", "https://i.postimg.cc/Gpk9jdjH/pokemon.jpg"));

        // 경매 시작 전 (3개)
        products.add(createProduct(members.get(1), "아이패드 Pro 12.9", "1주일 후 경매 시작",
                ProductCategory.DIGITAL_ELECTRONICS, 1100000L, now.plusHours(24), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/g09nRn3Z/ipad.jpg"));

        products.add(createProduct(members.get(2), "샤넬 클래식백", null,
                ProductCategory.FASHION_CLOTHING, 3500000L, now.plusHours(48), 48,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/tRDZfRHQ/tesla.jpg"));

        products.add(createProduct(members.get(3), "테슬라 모델3 2021년식", "저렴하게 판매합니다",
                ProductCategory.AUTOMOTIVE, 35000000L, now.plusHours(72), 48,
                DeliveryMethod.TRADE, "서울특별시 강남구", "https://i.postimg.cc/tRDZfRHQ/tesla.jpg"));

        // 낙찰 완료 (2개)
        Product sold1 = createProduct(members.get(1), "닌텐도 스위치 OLED", "낙찰 완료 상품",
                ProductCategory.DIGITAL_ELECTRONICS, 350000L, now.minusHours(25), 24,
                DeliveryMethod.DELIVERY, null, "https://i.postimg.cc/rpWRYSqw/switch.jpg");
        sold1.setStatus(AuctionStatus.SUCCESSFUL.getDisplayName());
        sold1.setEndTime(now.minusHours(1));
        sold1.setCurrentPrice(400000L);
        products.add(sold1);

        Product sold2 = createProduct(members.get(2), "다이슨 에어랩", "낙찰 완료 상품",
                ProductCategory.BEAUTY, 450000L, now.minusHours(26), 24,
                DeliveryMethod.BOTH, "서울특별시 마포구", "https://i.postimg.cc/2yTZ7rCm/dyson.jpg");
        sold2.setStatus(AuctionStatus.SUCCESSFUL.getDisplayName());
        sold2.setEndTime(now.minusHours(2));
        sold2.setCurrentPrice(520000L);
        products.add(sold2);

        // 유찰 (1개)
        Product failed = createProduct(members.get(3), "중고 청소기", "입찰 없어서 유찰",
                ProductCategory.HOME_LIVING, 50000L, now.minusHours(25), 24,
                DeliveryMethod.TRADE, "대구광역시 중구", "https://i.postimg.cc/4xyzw7K5/vacuum.jpg");
        failed.setStatus(AuctionStatus.FAILED.getDisplayName());
        failed.setEndTime(now.minusHours(1));
        products.add(failed);

        return products;
    }

    private Product createProduct(Member seller, String name, String desc, ProductCategory category,
                                  Long initialPrice, LocalDateTime startTime, int duration,
                                  DeliveryMethod deliveryMethod, String location, String imageUrl) {
        Product product = new StandardProduct(name, desc, category, initialPrice,
                startTime, duration, deliveryMethod, location, seller);
        product = productRepository.save(product);

        // 이미지 추가
        ProductImage image = new ProductImage(imageUrl, product);
        productImageRepository.save(image);
        product.addProductImage(image);

        return product;
    }

    // ========================================
    // 4. 입찰 생성
    // ========================================
    private void createBids(List<Member> members, List<Product> products) {
        // 경매 중인 상품에만 입찰
        for (Product product : products) {
            if (!product.getStatus().equals(AuctionStatus.BIDDING.getDisplayName())) {
                continue;
            }

            // 상품당 1~4개의 입찰 생성
            int bidCount = (int) (Math.random() * 4) + 1;
            Long currentPrice = product.getInitialPrice();

            for (int i = 0; i < bidCount; i++) {
                // 판매자가 아닌 랜덤 회원 선택
                Member bidder;
                do {
                    bidder = members.get((int) (Math.random() * members.size()));
                } while (bidder.equals(product.getSeller()));

                // 입찰가 증가 (50,000 ~ 250,000원)
                currentPrice += ThreadLocalRandom.current().nextLong(5, 26) * 10000;

                Bid bid = bidRepository.save(Bid.builder()
                        .product(product)
                        .member(bidder)
                        .bidPrice(currentPrice)
                        .status(BidStatus.BIDDING)
                        .build());

                product.addBid(bid);
            }

            product.setCurrentPrice(currentPrice);
            productRepository.save(product);
        }

        // 낙찰 완료 상품에 낙찰 입찰 생성
        for (Product product : products) {
            if (product.getStatus().equals(AuctionStatus.SUCCESSFUL.getDisplayName())) {
                Member winner = members.get((int) (Math.random() * (members.size() - 3)) + 3);

                Bid winningBid = bidRepository.save(Bid.builder()
                        .product(product)
                        .member(winner)
                        .bidPrice(product.getCurrentPrice())
                        .status(BidStatus.WINNING)
                        .build());

                product.addBid(winningBid);
            }
        }
    }

    // ========================================
    // 5. 결제 내역 생성
    // ========================================
    private void createPayments(List<Member> members) {
        for (int i = 1; i < Math.min(6, members.size()); i++) {
            Member member = members.get(i);
            Cash cash = cashRepository.findByMember(member).orElseThrow();
            PaymentMethod paymentMethod = paymentMethodRepository
                    .findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member)
                    .orElseThrow();

            // 각 회원당 1~3개의 충전 내역 생성
            int paymentCount = (int) (Math.random() * 3) + 1;

            for (int j = 0; j < paymentCount; j++) {
                long amount = ((long) (Math.random() * 5) + 1) * 100000L; // 100,000 ~ 500,000
                long beforeBalance = cash.getBalance();
                long afterBalance = beforeBalance + amount;

                // 지갑 잔액 업데이트
                cash.setBalance(afterBalance);
                cashRepository.save(cash);

                // 원장 기록
                CashTransaction transaction = cashTransactionRepository.save(
                        CashTransaction.builder()
                                .cash(cash)
                                .type(CashTxType.DEPOSIT)
                                .amount(amount)
                                .balanceAfter(afterBalance)
                                .relatedType(RelatedType.PAYMENT)
                                .relatedId((long) (2000 + i * 10 + j))
                                .build()
                );

                // 결제 기록
                paymentRepository.save(Payment.builder()
                        .member(member)
                        .paymentMethod(paymentMethod)
                        .amount(amount)
                        .currency("KRW")
                        .status(PaymentStatus.SUCCESS)
                        .provider("toss")
                        .methodType(paymentMethod.getMethodType())
                        .transactionId("pg_tx_demo_" + i + "_" + j)
                        .idempotencyKey("idem_demo_" + System.currentTimeMillis() + "_" + i + "_" + j)
                        .paidAt(LocalDateTime.now().minusDays(j + 1))
                        .cashTransaction(transaction)
                        .methodAlias(paymentMethod.getAlias())
                        .cardBrand(paymentMethod.getBrand())
                        .cardLast4(paymentMethod.getLast4())
                        .bankName(paymentMethod.getBankName())
                        .bankLast4(paymentMethod.getAcctLast4())
                        .build());
            }
        }
    }



    // ========================================
    // 6. 알림 생성
    // ========================================
    private void createNotifications(List<Member> members, List<Product> products) {
        LocalDateTime now = LocalDateTime.now();

        // 입찰 성공 알림
        for (int i = 0; i < Math.min(5, products.size()); i++) {
            Product product = products.get(i);
            if (product.getStatus().equals(AuctionStatus.BIDDING.getDisplayName())) {
                Member bidder = members.get((i % (members.size() - 2)) + 2);

                notificationQueueService.enqueueNotification(
                        bidder,
                        String.format("'%s' 상품에 입찰했습니다.", product.getProductName()),
                        "BID_SUCCESS",
                        product
                );
            }
        }

        // 입찰 밀림 알림
        for (int i = 0; i < Math.min(3, products.size()); i++) {
            Product product = products.get(i);
            if (product.getStatus().equals(AuctionStatus.BIDDING.getDisplayName())) {
                Member bidder = members.get((i % (members.size() - 2)) + 2);

                notificationQueueService.enqueueNotification(
                        bidder,
                        String.format("'%s' 상품에서 새로운 입찰이 들어와 밀렸습니다.", product.getProductName()),
                        "BID_OUTBID",
                        product
                );
            }
        }

        // 낙찰 알림
        for (Product product : products) {
            if (product.getStatus().equals(AuctionStatus.SUCCESSFUL.getDisplayName())) {
                // 낙찰자에게
                if (!product.getBids().isEmpty()) {
                    Member winner = product.getBids().get(0).getMember();

                    notificationQueueService.enqueueNotification(
                            winner,
                            String.format("축하합니다! '%s' 상품을 낙찰받았습니다!", product.getProductName()),
                            "AUCTION_WON",
                            product
                    );
                }

                // 판매자에게
                notificationQueueService.enqueueNotification(
                        product.getSeller(),
                        String.format("'%s' 상품이 낙찰되었습니다.", product.getProductName()),
                        "AUCTION_END",
                        product
                );
            }
        }

        // 경매 종료 임박 알림 (10분 후 발송 예약)
        for (int i = 0; i < Math.min(4, products.size()); i++) {
            Product product = products.get(i);
            if (product.getStatus().equals(AuctionStatus.BIDDING.getDisplayName())) {
                Member interested = members.get((i % (members.size() - 2)) + 2);

                notificationQueueService.enqueueScheduledNotification(
                        interested,
                        String.format("'%s' 경매가 10분 후 종료됩니다!", product.getProductName()),
                        "AUCTION_ENDING_SOON",
                        product,
                        now.plusMinutes(10)
                );
            }
        }

        // 대기중인 알림 (5분 후 발송 예약)
        for (int i = 0; i < 3; i++) {
            Product product = products.get(i % products.size());
            Member member = members.get((i % (members.size() - 2)) + 2);

            notificationQueueService.enqueueScheduledNotification(
                    member,
                    String.format("'%s' 상품에 관심을 가져주셔서 감사합니다.", product.getProductName()),
                    "PRODUCT_INTEREST",
                    product,
                    now.plusMinutes(5)
            );
        }
    }

    // ========================================
    // 7. 리뷰 생성
    // ========================================
    private void createReviews(List<Product> products) {
        // 낙찰 완료된 상품에 리뷰 작성
        for (Product product : products) {
            if (product.getStatus().equals(AuctionStatus.SUCCESSFUL.getDisplayName())) {
                if (!product.getBids().isEmpty()) {
                    Member reviewer = product.getBids().get(0).getMember();

                    // 긍정/부정 리뷰 랜덤
                    boolean isSatisfied = Math.random() > 0.2; // 80% 긍정

                    String[] positiveComments = {
                            "정말 좋은 거래였습니다! 상품 상태도 완벽하고 판매자님도 친절하셨어요.",
                            "설명대로 깨끗한 상품이었습니다. 감사합니다!",
                            "빠른 배송과 좋은 품질! 추천합니다.",
                            "가성비 최고! 다음에도 거래하고 싶어요.",
                            "완전 새것 같아요! 만족스러운 거래였습니다."
                    };

                    String[] negativeComments = {
                            "설명과 다르게 상태가 좋지 않았어요.",
                            "배송이 너무 늦었습니다.",
                            "기대했던 것보다 상태가 안 좋네요."
                    };

                    String comment = isSatisfied
                            ? positiveComments[(int) (Math.random() * positiveComments.length)]
                            : negativeComments[(int) (Math.random() * negativeComments.length)];

                    // Service를 통한 리뷰 생성
                    ReviewRequest reviewRequest = new ReviewRequest(
                            product.getId(),
                            comment,
                            isSatisfied
                    );

                    reviewService.createReview(reviewer.getId(), reviewRequest);
                }
            }
        }
    }
}