package com.backend.domain.review.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String comment;

    @Column(name = "is_satisfied", nullable = false)
    private Boolean isSatisfied;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private Member reviewer;
}