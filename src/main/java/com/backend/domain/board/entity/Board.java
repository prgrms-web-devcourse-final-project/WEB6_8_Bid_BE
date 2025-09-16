package com.backend.domain.board.entity;

import com.backend.domain.comment.entity.Comment;
import com.backend.domain.member.entity.Member;
import com.backend.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "boards")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Board extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(name = "board_type", length = 50)
    private String boardType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;
}