package com.backend.domain.member.controller;

import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.dto.MemberSignUpResponseDto;
import com.backend.domain.member.service.MemberService;
import com.backend.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1MemberController {
    private final MemberService memberService;

    @Operation(summary = "회원가입 API", description = "이메일 비밀번호를 받아 회원가입")
    @PostMapping("/auth/signup")
    public ResponseEntity<RsData<MemberSignUpResponseDto>> memberSignUp(@Valid @RequestBody MemberSignUpRequestDto memberSignUpRequestDto) {
        RsData<MemberSignUpResponseDto> MemberSignUpResponse = memberService.signup(memberSignUpRequestDto);

        return ResponseEntity.status(MemberSignUpResponse.statusCode()).body(MemberSignUpResponse);
    }

}
