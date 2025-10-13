package com.backend.global.initdata;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.service.BidService;
import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.product.dto.request.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.service.ProductImageService;
import com.backend.domain.product.service.StandardProductService;
import com.backend.domain.product.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Profile("test")
@Configuration
@RequiredArgsConstructor
public class TestInitData {
    @Autowired
    @Lazy
    private TestInitData self;

    private final StandardProductService productService;
    private final ProductImageService productImageService;
    private final MemberService memberService;
    private final BidService bidService;
    private final ProductSyncService productSyncService;

    @Bean
    ApplicationRunner testInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
            productSyncService.indexAllProducts();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        MemberSignUpRequestDto requestDto1 = new MemberSignUpRequestDto("system@example.com", "password123", "시스템", "010-1111-1111", "서울 강남구");
        memberService.signup(requestDto1);

        MemberSignUpRequestDto requestDto2 = new MemberSignUpRequestDto("admin@example.com", "password123", "관리자", "010-2222-2222", "서울 강남구");
        memberService.signup(requestDto2);

        MemberSignUpRequestDto requestDto3 = new MemberSignUpRequestDto("bidder1@example.com", "password123", "입찰자1", "010-3333-3333", "경기도 수원시");
        memberService.signup(requestDto3);

        MemberSignUpRequestDto requestDto4 = new MemberSignUpRequestDto("bidder2@example.com", "password123", "입찰자2", "010-4444-4444", "대구 중구");
        memberService.signup(requestDto4);

        MemberSignUpRequestDto requestDto5 = new MemberSignUpRequestDto("seller@example.com", "password123", "판매자", "010-5555-5555", "서울 서초구");
        memberService.signup(requestDto5);

        MemberSignUpRequestDto requestDto6 = new MemberSignUpRequestDto("user1@example.com", "password123", "전자기기왕", "010-6666-6666", "서울 마포구");
        memberService.signup(requestDto6);

        MemberSignUpRequestDto requestDto7 = new MemberSignUpRequestDto("user2@example.com", "password123", "명품러버", "010-7777-7777", "부산 해운대");
        memberService.signup(requestDto7);

        MemberSignUpRequestDto requestDto8 = new MemberSignUpRequestDto("user3@example.com", "password123", "나이키키", "010-8888-8888", "부산광역시 해운대구");
        memberService.signup(requestDto8);
    }

    @Transactional
    public void work2() {
        if (productService.count() > 0) return;

        Member member1 = memberService.findByNickname("입찰자1").get();
        Member member2 = memberService.findByNickname("입찰자2").get();
        Member member3 = memberService.findByNickname("판매자").get();
        Member member4 = memberService.findByNickname("전자기기왕").get();
        Member member5 = memberService.findByNickname("명품러버").get();
        Member member6 = memberService.findByNickname("나이키키").get();

        // 경매 중
        ProductCreateRequest requestDto1 = new ProductCreateRequest("iPhone 15 Pro", "최신 iPhone 15 Pro 새상품입니다.", 1, 1000000L, LocalDateTime.now().minusHours(1), "24시간", DeliveryMethod.BOTH, "서울 서초구");
        Product product1 = productService.saveProduct(member3, requestDto1);
        productImageService.createProductImage(product1, "/image1_1.jpg");

        ProductCreateRequest requestDto2 = new ProductCreateRequest("MacBook Pro M3", "MacBook Pro 14인치 M3 칩셋 모델입니다.", 1, 2000000L, LocalDateTime.now().minusMinutes(30), "48시간", DeliveryMethod.BOTH, "서울 서초구");
        Product product2 = productService.saveProduct(member3, requestDto2);
        productImageService.createProductImage(product2, "/image2_1.jpg");

        ProductCreateRequest requestDto3 = new ProductCreateRequest("AirPods Pro 2세대", "Apple AirPods Pro 2세대 노이즈캔슬링 이어폰입니다.", 1, 2000000L, LocalDateTime.now().minusMinutes(15), "24시간", DeliveryMethod.TRADE, "서울 홍대");
        Product product3 = productService.saveProduct(member3, requestDto3);
        productImageService.createProductImage(product3, "/image3_1.jpg");

        ProductCreateRequest requestDto4 = new ProductCreateRequest("아이폰 15 Pro 256GB", "미개봉 새 제품입니다. 직거래 선호합니다.", 1, 1000000L, LocalDateTime.now().minusHours(2), "24시간", DeliveryMethod.TRADE, "서울 강남구");
        Product product4 = productService.saveProduct(member4, requestDto4);
        productImageService.createProductImage(product4, "/image4_1.jpg");

        ProductCreateRequest requestDto5 = new ProductCreateRequest("갤럭시 S24 Ultra 512GB", null, 1, 1200000L, LocalDateTime.now().minusHours(1), "24시간", DeliveryMethod.DELIVERY, null);
        Product product5 = productService.saveProduct(member4, requestDto5);
        productImageService.createProductImage(product5, "/image5_1.jpg");

        ProductCreateRequest requestDto6 = new ProductCreateRequest("구찌 GG 마몽 숄더백", null, 2, 800000L, LocalDateTime.now().minusHours(1), "48시간", DeliveryMethod.TRADE, "부산 해운대");
        Product product6 = productService.saveProduct(member5, requestDto6);
        productImageService.createProductImage(product6, "/image6_1.jpg");

        ProductCreateRequest requestDto7 = new ProductCreateRequest("나이키 Air Max", null, 2, 700000L, LocalDateTime.now().minusHours(1), "24시간", DeliveryMethod.DELIVERY, null);
        Product product7 = productService.saveProduct(member6, requestDto7);
        productImageService.createProductImage(product7, "/image7_1.jpg");

        // 경매 시작 전
        ProductCreateRequest requestDto8 = new ProductCreateRequest("뉴발란스 스니커즈", null, 2, 700000L, LocalDateTime.now().plusHours(1), "24시간", DeliveryMethod.DELIVERY, null);
        Product product8 = productService.saveProduct(member6, requestDto8);
        productImageService.createProductImage(product8, "/image8_1.jpg");

        // 낙찰
        ProductCreateRequest requestDto9 = new ProductCreateRequest("닌텐도 Switch", null, 2, 700000L, LocalDateTime.now().minusHours(1), "24시간", DeliveryMethod.DELIVERY, null);
        Product product9 = productService.saveProduct(member4, requestDto9);
        productImageService.createProductImage(product9, "/image9_1.jpg");

        // 입찰 생성은 별도 트랜잭션으로 분리
        self.createBids(product4.getId(), product9.getId(), member1.getId(), member2.getId());
    }

    @Transactional
    public void createBids(Long product4Id, Long product9Id, Long member1Id, Long member2Id) {
        // 경매 진행
        bidService.createBid(product4Id, member1Id, new BidRequestDto(1200000L));
        bidService.createBid(product4Id, member2Id, new BidRequestDto(1300000L));

        bidService.createBid(product9Id, member1Id, new BidRequestDto(900000L));

        // 낙찰 처리
        productService.findById(product9Id).ifPresent(product -> {
            product.setStatus("낙찰");
            product.setEndTime(LocalDateTime.now());
        });
    }
}
