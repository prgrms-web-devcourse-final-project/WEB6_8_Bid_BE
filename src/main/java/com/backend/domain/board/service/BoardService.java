package com.backend.domain.board.service;

import com.backend.domain.board.dto.request.BoardWriteRequest;
import com.backend.domain.board.entity.Board;
import com.backend.domain.board.repository.BoardRepository;
import com.backend.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    // 게시글 생성 로직
    @Transactional
    public Board create(BoardWriteRequest request, Member member) {
        // 요청 DTO와 사용자 정보를 바탕으로 Board 엔티티 생성..
        Board board = request.toEntity(member);
        return boardRepository.save(board);
    }
}

