package com.backend.domain.member.controller;

import com.backend.domain.member.dto.*;
import com.backend.domain.member.service.MemberService;
import com.backend.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관련 API")
public class ApiV1MemberController {
    private final MemberService memberService;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${app.cookie.sameSite:Lax}")
    private String cookieSameSite;

    @Operation(summary = "회원가입 API", description = "이메일 비밀번호를 받아 회원가입")
    @PostMapping("/auth/signup")
    public ResponseEntity<RsData<MemberSignUpResponseDto>> memberSignUp(@Valid @RequestBody MemberSignUpRequestDto memberSignUpRequestDto) {
        RsData<MemberSignUpResponseDto> memberSignUpResponse = memberService.signup(memberSignUpRequestDto);

        return ResponseEntity.status(memberSignUpResponse.statusCode()).body(memberSignUpResponse);
    }

    @Operation(summary = "로그인 API", description = "이메일과 비밀번호를 받아 로그인 처리 후 토큰 발급")
    @PostMapping("/auth/login")
    public ResponseEntity<RsData<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
                                                          HttpServletResponse response) {
        RsData<LoginResponseDto> loginResponse = memberService.login(loginRequestDto);
        writeAuthCookies(response, loginResponse.data());
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

    @Operation(summary = "내 정보 수정 API", description = "내 정보 수정")
    @PutMapping("/members/me")
    public ResponseEntity<RsData<MemberMyInfoResponseDto>> myInfoModify(@RequestPart MemberModifyRequestDto memberModifyRequestDto, @RequestPart(required = false) MultipartFile profileImage, Authentication authentication) {
        RsData<MemberMyInfoResponseDto> myInfoModifyResponse = memberService.modify(authentication.getName(), memberModifyRequestDto, profileImage);
        return ResponseEntity.status(myInfoModifyResponse.statusCode()).body(myInfoModifyResponse);
    }

    @Operation(summary = "멤버 정보 조회", description = "특정 멤버의 정보를 조회합니다.")
    @GetMapping("/members/{id}")
    public ResponseEntity<RsData<MemberInfoResponseDto>> memberInfo(@PathVariable Long id) {
        MemberInfoResponseDto memberInfo = memberService.getMemberInfo(id);
        return ResponseEntity.ok(new RsData<>("200", "조회 성공", memberInfo));
    }

    @Operation(summary = "회원탈퇴 API", description = "현재 로그인된 회원을 탈퇴 처리합니다.")
    @DeleteMapping("/members/me")
    public ResponseEntity<RsData<Void>> memberWithdraw(Authentication authentication) {
        System.out.println("인증이름" + authentication.getName());
        RsData<Void> withdrawResult = memberService.withdraw(authentication.getName());
        return ResponseEntity.status(withdrawResult.statusCode()).body(withdrawResult);
    }

    // 로그인 성공 후 토큰을 안전한 쿠키로 내려줌..
    private void writeAuthCookies(HttpServletResponse res, LoginResponseDto dto) {
        // access 60분, refresh 7일
        ResponseCookie.ResponseCookieBuilder  accessBuilder = ResponseCookie.from("ACCESS_TOKEN", dto.accessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(60));

        ResponseCookie.ResponseCookieBuilder refreshBuilder = ResponseCookie.from("REFRESH_TOKEN", dto.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(7));

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            accessBuilder.domain(cookieDomain);
            refreshBuilder.domain(cookieDomain);
        }

        res.addHeader(HttpHeaders.SET_COOKIE, accessBuilder.build().toString());
        res.addHeader(HttpHeaders.SET_COOKIE, refreshBuilder.build().toString());
    }
}
