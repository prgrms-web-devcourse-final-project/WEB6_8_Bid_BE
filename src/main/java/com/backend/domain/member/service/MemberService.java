package com.backend.domain.member.service;

import com.backend.domain.member.dto.LoginRequestDto;
import com.backend.domain.member.dto.LoginResponseDto;
import com.backend.domain.member.dto.MemberModifyRequestDto;
import com.backend.domain.member.dto.MemberMyInfoResponseDto;
import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.dto.MemberSignUpResponseDto;
import com.backend.domain.member.dto.MemberInfoResponseDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.service.FileService;
import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsData;
import com.backend.global.security.JwtUtil;
import com.backend.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final FileService fileService;

    public RsData<MemberSignUpResponseDto> signup(MemberSignUpRequestDto memberSignUpRequestDto) {
        checkEmailDuplication(memberSignUpRequestDto.email());

        Member member = Member.builder()
                .email(memberSignUpRequestDto.email())
                .password(passwordEncoder.encode(memberSignUpRequestDto.password()))
                .nickname(memberSignUpRequestDto.nickname())
                .phoneNumber(memberSignUpRequestDto.phoneNumber())
                .address(memberSignUpRequestDto.address())
                .authority("ROLE_USER")
                .build();

        Member savedMember = memberRepository.save(member);

        MemberSignUpResponseDto responseDto = new MemberSignUpResponseDto(savedMember.getId(), savedMember.getEmail(), savedMember.getNickname());
        return new RsData<>("200-1", "회원가입이 완료되었습니다.", responseDto);
    }

    public RsData<LoginResponseDto> login(LoginRequestDto loginRequestDto) {
        Member member = findMemberByEmail(loginRequestDto.email());
        verifyPassword(loginRequestDto.password(), member.getPassword());

        String accessToken = jwtUtil.generateAccessToken(member.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(member.getEmail());

        member.updateRefreshToken( refreshToken);
        memberRepository.save(member);

        return new RsData<>("200-2", "로그인 성공", new LoginResponseDto(accessToken, refreshToken));
    }

    public void logout(String accessToken) {
        long remainingExpirationMillis = jwtUtil.getRemainingExpirationMillis(accessToken);
        redisUtil.setData(accessToken, "logout", remainingExpirationMillis);
    }

    public RsData<LoginResponseDto> reissue(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token 입니다.");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        Member member = findMemberByEmail(email);

        if (!member.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(member.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(member.getEmail());

        member.updateRefreshToken(newRefreshToken);

        return new RsData<>("200-3", "토큰 재발급 성공", new LoginResponseDto(newAccessToken, newRefreshToken));
    }

    @Transactional(readOnly = true)
    public RsData<MemberMyInfoResponseDto> getMyInfo(String email) {
        Member member = findMemberByEmail(email);
        MemberMyInfoResponseDto responseDto = new MemberMyInfoResponseDto(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getAddress(),
                member.getProfileImageUrl(),
                member.getCreditScore(),
                member.getCreateDate(),
                member.getModifyDate()
        );
        return new RsData<>("200-1", "내 정보가 조회되었습니다.", responseDto);
    }

    public RsData<MemberMyInfoResponseDto> modify(String email, MemberModifyRequestDto memberModifyRequestDto, MultipartFile profileImage) {
        Member member = findMemberByEmail(email);

        String profileImageUrl = "";
        if (profileImage != null && !profileImage.isEmpty()) {
            profileImageUrl = fileService.uploadFile(profileImage, "member");
        }

        member.updateProfile(
                memberModifyRequestDto.nickname(),
                profileImageUrl,
                memberModifyRequestDto.phoneNumber(),
                memberModifyRequestDto.address()
        );

        Member modifiedMember = memberRepository.save(member);

        MemberMyInfoResponseDto responseDto = new MemberMyInfoResponseDto(
                modifiedMember.getId(),
                modifiedMember.getEmail(),
                modifiedMember.getNickname(),
                modifiedMember.getPhoneNumber(),
                modifiedMember.getAddress(),
                modifiedMember.getProfileImageUrl(),
                modifiedMember.getCreditScore(),
                modifiedMember.getCreateDate(),
                modifiedMember.getModifyDate()
        );

        return new RsData<>("200-4", "내 정보가 수정되었습니다.", responseDto);
    }

    private void checkEmailDuplication(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }
    }

    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceException("404", "존재하지 않는 이메일입니다."));
    }

    private void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
    }

    public long count() {
        return memberRepository.count();
    }

    public Optional<Member> findByNickname(String nickname) {
        return memberRepository.findByNickname(nickname);
    }

    public Optional<Member> findById(Long memberId) {
        return memberRepository.findById(memberId);
    }

    @Transactional(readOnly = true)
    public MemberInfoResponseDto getMemberInfo(Long memberId) {
        Member member = findById(memberId)
                .orElseThrow(() -> new ServiceException("404", "존재하지 않는 유저입니다."));
        return new MemberInfoResponseDto(member);
    }
}
