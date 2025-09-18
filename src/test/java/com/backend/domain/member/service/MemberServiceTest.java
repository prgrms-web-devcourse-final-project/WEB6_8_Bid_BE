package com.backend.domain.member.service;

import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.dto.MemberSignUpResponseDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // 테스트 후 데이터 롤백
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void t1() {
        // Given: 회원가입 요청 데이터가 주어졌을 때
        MemberSignUpRequestDto requestDto = new MemberSignUpRequestDto(
                "test@example.com",
                "password123",
                "testUser",
                "010-1234-5678",
                "Seoul, South Korea"
        );

        // When: 회원가입을 시도하면
        RsData<MemberSignUpResponseDto> rsData = memberService.signup(requestDto);

        // Then: 성공 응답을 받고, 데이터베이스에 회원이 저장되어야 한다.
        assertThat(rsData.resultCode()).isEqualTo("200-1");
        assertThat(rsData.msg()).isEqualTo("회원가입이 완료되었습니다.");
        assertThat(rsData.data()).isNotNull();
        assertThat(rsData.data().email()).isEqualTo("test@example.com");
        assertThat(rsData.data().nickname()).isEqualTo("testUser");

        // DB에 실제로 저장되었는지 확인
        Member foundMember = memberRepository.findByEmail("test@example.com").orElse(null);
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getNickname()).isEqualTo("testUser");

        // 비밀번호가 암호화되어 저장되었는지 확인
        assertThat(passwordEncoder.matches("password123", foundMember.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 테스트 (이메일 중복)")
    void t2() {
        // Given: 동일한 이메일로 이미 가입된 회원이 있을 때
        MemberSignUpRequestDto initialRequest = new MemberSignUpRequestDto(
                "duplicate@example.com",
                "password123",
                "user1",
                "010-1111-1111",
                "Busan, South Korea"
        );
        memberService.signup(initialRequest);

        // When: 같은 이메일로 다시 회원가입을 시도하면
        MemberSignUpRequestDto duplicateRequest = new MemberSignUpRequestDto(
                "duplicate@example.com",
                "newPassword456",
                "user2",
                "010-2222-2222",
                "Incheon, South Korea"
        );
        RsData<MemberSignUpResponseDto> rsData = memberService.signup(duplicateRequest);

        // Then: 실패 응답을 받아야 한다.
        assertThat(rsData.resultCode()).isEqualTo("400-1");
        assertThat(rsData.msg()).isEqualTo("이미 사용중인 이메일입니다.");
        assertThat(rsData.data()).isNull();
    }
}