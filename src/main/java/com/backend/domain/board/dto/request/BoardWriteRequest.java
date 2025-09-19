package com.backend.domain.board.dto.request;

import com.backend.domain.board.constant.BoardType;
import com.backend.domain.board.entity.Board;
import com.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardWriteRequest {

    private String title;
    private String content;
    private BoardType boardType;

    // DTO를 Board 엔티티로 변환하는 메서드
    public Board toEntity(Member member) {
        return new Board(this.title, this.content, this.boardType, member, null);
    }
}
