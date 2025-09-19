package com.backend.domain.board.controller;

import com.backend.domain.board.dto.request.BoardWriteRequest;
import com.backend.domain.board.dto.response.BoardWriteResponse;
import com.backend.domain.board.entity.Board;
import com.backend.domain.board.service.BoardService;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards")
@Tag(name = "게시글", description = "게시글 작성, 조회, 수정, 삭제 관련 API")
public class ApiV1BoardController {

    private final BoardService boardService;
    private final MemberRepository memberRepository; // MemberRepository 주입


    @PostMapping
    @Operation(summary = "게시글 작성")
    @ApiResponse(responseCode = "201", description = "게시글이 작성되었습니다.")
    public ResponseEntity<RsData<BoardWriteResponse>> writeBoard(@RequestBody BoardWriteRequest request) {

        // 현재는 임시 Member 객체를 생성하여 사용..
        Member member = memberRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("1번 회원을 찾을 수 없습니다."));

        // 게시글 생성..
        Board createdBoard = boardService.create(request, member);

        // 응답 DTO 생성..
        BoardWriteResponse data = BoardWriteResponse.from(createdBoard);

        // 최종 응답 형태 생성..
        RsData<BoardWriteResponse> response = new RsData<>("201", "게시글이 작성되었습니다", data);
        return ResponseEntity.status(201).body(response);
    }
}

