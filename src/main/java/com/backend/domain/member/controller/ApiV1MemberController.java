package com.backend.domain.member.controller;

import com.backend.domain.member.dto.LoginRequestDto;
import com.backend.domain.member.dto.LoginResponseDto;
import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.dto.MemberSignUpResponseDto;
import com.backend.domain.member.service.MemberService;
import com.backend.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        RsData<MemberSignUpResponseDto> memberSignUpResponse = memberService.signup(memberSignUpRequestDto);

        return ResponseEntity.status(memberSignUpResponse.statusCode()).body(memberSignUpResponse);
    }

    @Operation(summary = "로그인 API", description = "이메일과 비밀번호를 받아 로그인 처리 후 토큰 발급")
    @PostMapping("/auth/login")
    public ResponseEntity<RsData<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        RsData<LoginResponseDto> loginResponse = memberService.login(loginRequestDto);

        return ResponseEntity.status(loginResponse.statusCode()).body(loginResponse);
    }
}
