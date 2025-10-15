package com.backend.global.initdata;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.enums.BidStatus;
import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.service.NotificationQueueService;
import com.backend.domain.product.entity.Product;
import com.backend.domain.product.entity.ProductImage;
import com.backend.domain.product.entity.StandardProduct;
import com.backend.domain.product.enums.AuctionStatus;
import com.backend.domain.product.enums.DeliveryMethod;
import com.backend.domain.product.enums.ProductCategory;
import com.backend.domain.product.repository.jpa.ProductImageRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class ProdInitData {

    @Autowired
    @Lazy
    private ProdInitData self;

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BidRepository bidRepository;
    private final ProductSyncService productSyncService;
    private final NotificationQueueService notificationQueueService;

    @Bean
    ApplicationRunner prodInitDataApplicationRunner() {
        return args -> {
            self.work1();
            productSyncService.indexAllProducts();
        };
    }

    @Transactional
    public void work1() {
        if (memberRepository.findByEmail("user5@test.com").isPresent()) {
            return;
        }

        if (memberRepository.findByEmail("member2@example.com").isEmpty()) {
            return;
        }

        Member member1 = memberRepository.findByEmail("member2@example.com").get();

        Member member2 = Member.builder()
                .email("user5@test.com")
                .password(passwordEncoder.encode("12341234"))
                .nickname("입찰자2")
                .phoneNumber("010-2222-2222")
                .address("서울특별시 강남구")
                .authority("ROLE_USER")
                .creditScore(50)
                .products(new ArrayList<>())
                .bids(new ArrayList<>())
                .notifications(new ArrayList<>())
                .boards(new ArrayList<>())
                .comments(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();
        memberRepository.save(member2);


        Product product = createProduct(member1, "아이폰 17 pro", "미개봉 상품입니다!",
                ProductCategory.DIGITAL_ELECTRONICS, 200000L, LocalDateTime.now().minusHours(25), 24,
                DeliveryMethod.DELIVERY, "서울 강남구", "https://i.postimg.cc/85pkBWcS/iphone15-1.jpg");
        product.setStatus(AuctionStatus.SUCCESSFUL.getDisplayName());
        product.setEndTime(LocalDateTime.now().minusHours(1));
        product.setCurrentPrice(220000L);


        Bid winningBid = bidRepository.save(Bid.builder()
                .product(product)
                .member(member2)
                .bidPrice(220000L)
                .status(BidStatus.WINNING)
                .build());

        product.addBid(winningBid);


        // 입찰 성공 알림
        notificationQueueService.enqueueNotification(
                member2,
                "'아이폰 17 pro' 상품에 220,000원으로 입찰했습니다.",
                "BID_SUCCESS",
                product
        );

        // 낙찰 알림
        notificationQueueService.enqueueNotification(
                member2,
                "축하합니다! '아이폰 17 pro' 상품을 낙찰받았습니다!",
                "AUCTION_WON",
                product
        );

        notificationQueueService.enqueueNotification(
                member1,
                "'아이폰 17 pro' 상품이 낙찰되었습니다.",
                "AUCTION_END",
                product
        );
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
}