package com.backend.domain.board.dto.response;

import com.backend.domain.board.constant.BoardType;
import com.backend.domain.board.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardWriteResponse {

    private Long id;
    private String title;
    private String content;
    private BoardType boardType;

    // Board 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static BoardWriteResponse from(Board board) {
        return BoardWriteResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .boardType(board.getBoardType())
                .build();
    }
}

