package com.backend.domain.board.service;

import com.backend.domain.board.constant.BoardType;
import com.backend.domain.board.dto.request.BoardWriteRequest;
import com.backend.domain.board.entity.Board;
import com.backend.domain.board.repository.BoardRepository;
import com.backend.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @InjectMocks
    private BoardService boardService;

    @Mock
    private BoardRepository boardRepository;

    @Test
    @DisplayName("게시글 생성 로직 테스트")
    void createBoard_success() {
        // given..
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);

        BoardWriteRequest request = new BoardWriteRequest(
                "테스트 제목",
                "테스트 내용입니다.",
                BoardType.NOTICE
        );
        Board savedBoard = request.toEntity(member);
        given(boardRepository.save(any(Board.class))).willReturn(savedBoard);


        // when..
        Board resultBoard = boardService.create(request, member);

        // then..
        // 반환된 결과가 null이 아닌지 확인..
        assertThat(resultBoard).isNotNull();
        // 요청했던 제목과 반환된 제목이 일치하는지 확인..
        assertThat(resultBoard.getTitle()).isEqualTo(request.getTitle());
        // 요청했던 내용과 반환된 내용이 일치하는지 확인..
        assertThat(resultBoard.getContent()).isEqualTo(request.getContent());
        // 작성자 정보가 올바르게 연결되었는지 확인..
        assertThat(resultBoard.getMember().getId()).isEqualTo(1L);
        verify(boardRepository).save(any(Board.class));
    }
}

