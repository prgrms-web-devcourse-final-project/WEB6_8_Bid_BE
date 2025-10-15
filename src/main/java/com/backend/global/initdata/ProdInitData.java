package com.backend.global.initdata;

import com.backend.domain.bid.repository.BidRepository;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.notification.service.NotificationQueueService;
import com.backend.domain.product.entity.Product;
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
        if (memberRepository.findByEmail("member2@example.com").isEmpty()) {
            return;
        }

        Product product = productRepository.findById(24L).get();
        if (product.getEndTime().isAfter(LocalDateTime.of(2025, 10, 16, 0, 9, 59))) {
            return;
        }
        product.setEndTime(LocalDateTime.of(2025, 10, 16, 0, 9, 59));
    }
}