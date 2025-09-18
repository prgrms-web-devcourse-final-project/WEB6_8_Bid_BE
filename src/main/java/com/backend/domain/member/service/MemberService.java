package com.backend.domain.member.service;

import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.dto.MemberSignUpResponseDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public RsData<MemberSignUpResponseDto> signup(MemberSignUpRequestDto memberSignUpRequestDto) {
        // 1. 이메일 중복 체크
        if (memberRepository.findByEmail(memberSignUpRequestDto.email()).isPresent()) {
            return new RsData<>("400-1", "이미 사용중인 이메일입니다.");
        }

        // 2. 회원 객체 생성 및 비밀번호 암호화
        Member member = new Member();
        member.setEmail(memberSignUpRequestDto.email());
        member.setPassword(passwordEncoder.encode(memberSignUpRequestDto.password()));
        member.setNickname(memberSignUpRequestDto.nickname());
        member.setPhoneNumber(memberSignUpRequestDto.phone());
        member.setAddress(memberSignUpRequestDto.address());
        member.setAuthority("ROLE_USER"); // 기본 권한 설정

        // 3. 회원 정보 저장
        Member savedMember = memberRepository.save(member);

        // 4. 응답 DTO 생성 및 반환
        MemberSignUpResponseDto responseDto = new MemberSignUpResponseDto(savedMember.getId(), savedMember.getEmail(), savedMember.getNickname());
        return new RsData<>("200-1", "회원가입이 완료되었습니다.", responseDto);
    }
}
