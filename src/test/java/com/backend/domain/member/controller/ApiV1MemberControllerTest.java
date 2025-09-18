package com.backend.domain.member.controller;

import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ApiV1MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 데이터 정리
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공")
    void t1() throws Exception {
        // Given
        MemberSignUpRequestDto requestDto = new MemberSignUpRequestDto(
                "test@example.com",
                "password123",
                "testUser",
                "01012345678",
                "Test Address"
        );

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("testUser"));
    }

    @Test
    @DisplayName("중복된 이메일로 인한 회원가입 실패")
    void t2() throws Exception {
        // Given
        // 먼저 회원가입을 한번 실행해서 이메일을 선점
        MemberSignUpRequestDto existingUserDto = new MemberSignUpRequestDto(
                "test@example.com",
                "password123",
                "existingUser",
                "01087654321",
                "Existing Address"
        );
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingUserDto)));

        // 동일한 이메일로 다시 회원가입 시도
        MemberSignUpRequestDto newUserDto = new MemberSignUpRequestDto(
                "test@example.com",
                "newPassword",
                "newUser",
                "01012345678",
                "New Address"
        );

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest()) // 컨트롤러가 RsData의 statusCode에 따라 400을 반환
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("이미 사용중인 이메일입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}