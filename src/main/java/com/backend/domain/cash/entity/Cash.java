package com.backend.domain.cash.entity;

import com.backend.domain.member.entity.Member;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cash extends BaseEntity {

    // 이 지갑의 주인..
    @OneToOne(fetch = FetchType.LAZY)                   // 회원 1명 ↔ 지갑 1개 (1:1)..
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 돈(잔액)..
    @Column(nullable = false)
    private Long balance;                               // 현재 잔액(원) — 항상 0 이상..
}
