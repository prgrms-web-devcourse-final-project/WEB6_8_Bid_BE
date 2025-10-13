package com.backend.domain.member.entity;

import com.backend.domain.bid.entity.Bid;
import com.backend.domain.board.entity.Board;
import com.backend.domain.comment.entity.Comment;
import com.backend.domain.notification.entity.Notification;
import com.backend.domain.product.entity.Product;
import com.backend.domain.review.entity.Review;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String authority;

    private String profileImageUrl;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    private int creditScore;

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateProfile(String nickname, String profileImageUrl, String phone, String address) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phone;
        this.address = address;
    }

    public void updateCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Board> boards;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;

    @OneToMany(mappedBy = "reviewer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}