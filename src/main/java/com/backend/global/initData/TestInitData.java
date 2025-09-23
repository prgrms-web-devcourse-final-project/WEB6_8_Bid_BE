package com.backend.global.initData;

import com.backend.domain.bid.dto.BidRequestDto;
import com.backend.domain.bid.service.BidService;
import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.domain.product.dto.ProductCreateRequest;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.service.ProductService;
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

    private final ProductService productService;
    private final MemberService memberService;
    private final BidService bidService;

    @Bean
    ApplicationRunner testInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        MemberSignUpRequestDto requestDto1 = new MemberSignUpRequestDto("user1@example.com", "1234", "전자기기왕", "010-1111-1111", "서울 강남구");
        memberService.signup(requestDto1);

        MemberSignUpRequestDto requestDto2 = new MemberSignUpRequestDto("user2@example.com", "1234", "명품러버", "010-2222-2222", "부산 해운대");
        memberService.signup(requestDto2);

        MemberSignUpRequestDto requestDto3 = new MemberSignUpRequestDto("user3@example.com", "1234", "나이키키", "010-3333-3333", "부산광역시 해운대구");
        memberService.signup(requestDto3);

        MemberSignUpRequestDto requestDto4 = new MemberSignUpRequestDto("user4@example.com", "1234", "입찰자1", "010-4444-4444", "경기도 수원시");
        memberService.signup(requestDto4);

        MemberSignUpRequestDto requestDto5 = new MemberSignUpRequestDto("user5@example.com", "1234", "입찰자2", "010-5555-5555", "대구 중구");
        memberService.signup(requestDto5);
    }

    @Transactional
    public void work2() {
        if (productService.count() > 0) return;

        Member member1 = memberService.findByNickname("전자기기왕").get();
        Member member2 = memberService.findByNickname("명품러버").get();
        Member member3 = memberService.findByNickname("나이키키").get();
        Member member4 = memberService.findByNickname("입찰자1").get();
        Member member5 = memberService.findByNickname("입찰자2").get();

        ProductCreateRequest requestDto1 = new ProductCreateRequest("아이폰 15 Pro 256GB", "미개봉 새 제품입니다. 직거래 선호합니다.", 1, 1000000L, LocalDateTime.of(2024, 12, 17, 9, 0), "24시간", DeliveryMethod.TRADE, "서울 강남구");
        Product product1 = productService.createProduct(member1, requestDto1);
        productService.createProductImage(product1, "/image1_1.jpg");

        ProductCreateRequest requestDto2 = new ProductCreateRequest("갤럭시 S24 Ultra 512GB", null, 1, 1200000L, LocalDateTime.of(2024, 12, 17, 10, 0), "24시간", DeliveryMethod.DELIVERY, null);
        Product product2 = productService.createProduct(member1, requestDto2);
        productService.createProductImage(product2, "/image2_1.jpg");

        ProductCreateRequest requestDto3 = new ProductCreateRequest("구찌 GG 마몽 숄더백", null, 2, 800000L, LocalDateTime.of(2024, 12, 17, 10, 0), "48시간", DeliveryMethod.TRADE, "부산 해운대");
        Product product3 = productService.createProduct(member2, requestDto3);
        productService.createProductImage(product3, "/image3_1.jpg");

        ProductCreateRequest requestDto4 = new ProductCreateRequest("나이키 Air Max", null, 2, 700000L, LocalDateTime.of(2024, 12, 17, 10, 0), "24시간", DeliveryMethod.DELIVERY, null);
        Product product4 = productService.createProduct(member3, requestDto4);
        productService.createProductImage(product4, "/image4_1.jpg");


        product1.setStatus("경매 중");
        product2.setStatus("경매 중");
        product3.setStatus("경매 중");
        product4.setStatus("경매 중");
        bidService.createBid(product1.getId(), member4.getId(), new BidRequestDto(1200000L));
        bidService.createBid(product1.getId(), member5.getId(), new BidRequestDto(1300000L));
    }
}
