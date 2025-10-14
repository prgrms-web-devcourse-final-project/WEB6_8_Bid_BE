package com.backend.domain.review.entity;

import com.backend.domain.member.entity.Member;
import com.backend.domain.product.entity.Product;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String comment;

    @Column(name = "is_satisfied", nullable = false)
    private Boolean isSatisfied;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Member reviewer;

    public void update(String comment, Boolean isSatisfied) {
        this.comment = comment;
        this.isSatisfied = isSatisfied;
    }
}