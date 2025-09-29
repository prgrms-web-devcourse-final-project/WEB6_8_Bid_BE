package com.backend.domain.member.controller;

import com.backend.domain.member.dto.*;
import com.backend.domain.member.service.MemberService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관련 API")
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

    @Operation(summary = "로그아웃 API", description = "accessToken을 받아 로그아웃 처리")
    @PostMapping("/auth/logout")
    public ResponseEntity<LogoutResponseDto> logout(@RequestHeader("Authorization") String accessToken) {
        String token = accessToken.substring(7);
        memberService.logout(token);

        LogoutResponseDto response = new LogoutResponseDto("200", "로그아웃 되었습니다.");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 재생성 API", description = "refreshToken을 받아서 AccessToken 재발급")
    @PostMapping("/auth/reissue")
    public ResponseEntity<RsData<LoginResponseDto>> reissue(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.substring(7);
        RsData<LoginResponseDto> reissueResponse = memberService.reissue(token);

        return ResponseEntity.status(reissueResponse.statusCode()).body(reissueResponse);
    }

    @Operation(summary = "테스트용 API", description = "인증된 사용자의 이메일 반환")
    @GetMapping("/members/test")
    public ResponseEntity<String> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(authentication.getName());
    }

    @Operation(summary = "로그인 확인 API", description = "현재 로그인 되어있는지 확인")
    @GetMapping("/auth/check")
    public ResponseEntity<RsData<String>> checkLogin(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(new RsData<>("200-1", "로그인 되어 있습니다.", authentication.getName()));
        } else {
            return ResponseEntity.ok(new RsData<>("200-2", "로그아웃 상태입니다.", null));
        }
    }

    @Operation(summary = "내 정보 API", description = "내 정보 확인")
    @GetMapping("/members/me")
    public ResponseEntity<RsData<MemberMyInfoResponseDto>> myInfo(Authentication authentication) {
        RsData<MemberMyInfoResponseDto> myInfoResponse = memberService.getMyInfo(authentication.getName());
        return ResponseEntity.ok(myInfoResponse);
    }

    @Operation(summary = "내 정보 수정 Mock API", description = "내 정보 수정")
    @PutMapping("/members/me")
    public ResponseEntity<RsData<MemberMyInfoResponseDto>> myInfoModify(@RequestBody MemberModifyRequestDto memberModifyRequestDto, Authentication authentication) {
        return ResponseEntity.ok(new RsData<>("200-1", "내 정보가 수정되었습니다.",
                new MemberMyInfoResponseDto(1L, "test@test.com", "test", "010-1111-1111",
                        "서울특별시 영등포구...", "https://example.com/profile.jpg", 50, LocalDateTime.now(), LocalDateTime.now())
        ));
    }

    @Operation(summary = "판매자 정보 Mock API", description = "판매자 정보 확인")
    @GetMapping("/members/{id}")
    public ResponseEntity<RsData<MemberInfoResponseDto>> memberInfo(@PathVariable Long id) {
        return ResponseEntity.ok(new RsData<>("200-1", "판매자 정보가 조회되었습니다.",
                new MemberInfoResponseDto(2L, "test@test.com", "test", "010-1111-1111",
                        "https://example.com/profile.jpg", 50, LocalDateTime.now())
        ));
    }

    @Operation(summary = "회원탈퇴 Mock API", description = "회원탈퇴 확인")
    @DeleteMapping("/members/me")
    public ResponseEntity<RsData<String>> memberWithdraw(Authentication authentication) {
        return ResponseEntity.ok(new RsData<>("200-1", "회원 탈퇴가 완료되었습니다.", null));
    }
}
