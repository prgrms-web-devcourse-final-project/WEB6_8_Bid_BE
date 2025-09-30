package com.backend.global.testdata.generator;

import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.dto.MemberSignUpResponseDto;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.service.MemberService;
import com.backend.global.response.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
@Slf4j
public class MemberTestDataGenerator {
    
    private final MemberService memberService;
    private final Faker faker = new Faker(new Locale("ko"));
    
    public List<Member> generate(int count) {
        log.info("회원 데이터 생성 시작: {}개", count);

        List<Member> members = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            MemberSignUpRequestDto request = new MemberSignUpRequestDto(
                    faker.internet().emailAddress(),
                    "password123",
                    faker.name().name(),
                    faker.phoneNumber().cellPhone(),
                    faker.address().fullAddress()
            );

            try {
                RsData<MemberSignUpResponseDto> result = memberService.signup(request);
                if ("200-1".equals(result.resultCode())) {
                    memberService.findById(result.data().memberId())
                            .ifPresent(members::add);
                }

                if ((i + 1) % 100 == 0) {
                    log.info("진행률: {}/{}", i + 1, count);
                }
                
            } catch (Exception e) {
                log.warn("회원 생성 실패 ({}번째): {}", i + 1, e.getMessage());
            }
        }

        return members;
    }
}