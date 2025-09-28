package com.backend.global.initdata;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BaseInitData  {

    @Autowired
    @Lazy
    private BaseInitData self;
    private final BidRepository bidRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner(){
        return args -> {
            self.work1();
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
                .password("password123")
                .nickname("입찰자1")
                .build());

        Member bidder2 = memberRepository.save(Member.builder()
                .email("bidder2@example.com")
                .password("password123")
                .nickname("입찰자2")
                .build());

        Member seller = memberRepository.save(Member.builder()
                .email("seller@example.com")
                .password("password123")
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
                .status("bidding")
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(1100000L)
                .product(product1)
                .member(bidder2)
                .status("bidding")
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(2100000L)
                .product(product2)
                .member(bidder2)
                .status("bidding")
                .build());

        bidRepository.save(Bid.builder()
                .bidPrice(220000L)
                .product(product3)
                .member(bidder1)
                .status("bidding")
                .build());
    }
}
