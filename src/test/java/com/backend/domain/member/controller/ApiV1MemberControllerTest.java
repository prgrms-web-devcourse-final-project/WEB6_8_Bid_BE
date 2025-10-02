package com.backend.domain.member.controller;

import com.backend.domain.member.dto.LoginRequestDto;
import com.backend.domain.member.dto.MemberModifyRequestDto;
import com.backend.domain.member.dto.MemberSignUpRequestDto;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.product.service.FileService;
import com.backend.global.redis.TestRedisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cloud.aws.credentials.access-key=",
        "cloud.aws.credentials.secret-key=",
        "pg.toss.secretKey=test_sk_1234567890"
})
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class ApiV1MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private FileService fileService;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 데이터 정리
        memberRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 실행 후에 Redis 데이터 정리
        redisTemplate.getConnectionFactory().getConnection().flushAll();
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
                .andExpect(status().isConflict()) // GlobalExceptionHandler에 의해 409 Conflict
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("이미 사용중인 이메일입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("로그인 성공")
    void t3() throws Exception {
        // Given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "test@example.com", "password123", "testUser", "01012345678", "Test Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "password123");

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.msg").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void t4() throws Exception {
        // Given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "test@example.com", "password123", "testUser", "01012345678", "Test Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "wrong_password");

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isConflict()) // GlobalExceptionHandler에 의해 409 Conflict
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void t5() throws Exception {
        // Given
        LoginRequestDto loginDto = new LoginRequestDto("nonexistent@example.com", "password123");

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 이메일입니다."));
    }

    @Test
    @DisplayName("로그아웃 성공 및 토큰 무효화 확인")
    void t6() throws Exception {
        // Given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "test@example.com", "password123", "testUser", "01012345678", "Test Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        // 로그인하여 토큰 발급
        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));

        String responseBody = loginResult.andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();

        // When (로그아웃)
        ResultActions logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then (로그아웃 응답 확인)
        logoutResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."));

        // When (로그아웃된 토큰으로 보호된 API 접근 시도)
        ResultActions accessResult = mockMvc.perform(get("/api/v1/members/test")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then (접근 거부 확인)
        accessResult.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void t7() throws Exception {
        // Given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "test@example.com", "password123", "testUser", "01012345678", "Test Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        // 로그인하여 토큰 발급
        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));

        String loginResponseBody = loginResult.andReturn().getResponse().getContentAsString();
        String originalRefreshToken = objectMapper.readTree(loginResponseBody).get("data").get("refreshToken").asText();

        // When
        ResultActions reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
                        .header("Authorization", "Bearer " + originalRefreshToken))
                .andDo(print());

        // Then
        reissueResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-3"))
                .andExpect(jsonPath("$.msg").value("토큰 재발급 성공"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

        String reissueResponseBody = reissueResult.andReturn().getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(reissueResponseBody).get("data").get("accessToken").asText();
        String newRefreshToken = objectMapper.readTree(reissueResponseBody).get("data").get("refreshToken").asText();

        // 토큰이 실제로 변경되었는지 확인
        assert !newRefreshToken.equals(originalRefreshToken);
    }

    @Test
    @DisplayName("로그인 확인 - 인증된 사용자")
    void t8() throws Exception {
        // Given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "test@example.com", "password123", "testUser", "01012345678", "Test Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        // 로그인하여 토큰 발급
        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));

        String responseBody = loginResult.andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/check")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그인 되어 있습니다."))
                .andExpect(jsonPath("$.data").value("test@example.com"));
    }

    @Test
    @DisplayName("로그인 확인 - 미인증 사용자")
    void t9() throws Exception {
        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/check"))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.msg").value("로그아웃 상태입니다."));
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void t10() throws Exception {
        // Given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "myinfo@example.com", "password123", "myinfoUser", "01011112222", "My Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        // 로그인하여 토큰 발급
        LoginRequestDto loginDto = new LoginRequestDto("myinfo@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));

        String responseBody = loginResult.andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();

        // When
        ResultActions resultActions = mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보가 조회되었습니다."))
                .andExpect(jsonPath("$.data.email").value("myinfo@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("myinfoUser"))
                .andExpect(jsonPath("$.data.address").value("My Address"));
    }

    @Test
    @DisplayName("내 정보 수정 성공")
    void t11() throws Exception {
        // given
        given(fileService.uploadFile(any(), any())).willReturn("https://test.com/modified.jpg");

        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "modify@example.com", "password123", "beforeModify", "01011112222", "Before Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        // 로그인하여 토큰 발급
        LoginRequestDto loginDto = new LoginRequestDto("modify@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));

        String responseBody = loginResult.andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();

        MemberModifyRequestDto modifyDto = new MemberModifyRequestDto(
                "afterModify", "01099998888", "After Address");

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        MockMultipartFile modifyDtoPart = new MockMultipartFile(
                "memberModifyRequestDto",
                "",
                "application/json",
                objectMapper.writeValueAsString(modifyDto).getBytes(StandardCharsets.UTF_8)
        );


        // When
        ResultActions resultActions = mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/me")
                        .file(profileImage)
                        .file(modifyDtoPart)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-4"))
                .andExpect(jsonPath("$.msg").value("내 정보가 수정되었습니다."))
                .andExpect(jsonPath("$.data.nickname").value("afterModify"))
                .andExpect(jsonPath("$.data.phoneNumber").value("01099998888"))
                .andExpect(jsonPath("$.data.address").value("After Address"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://test.com/modified.jpg"));
    }

    @Test
    @DisplayName("멤버 정보 조회 성공")
    void t12() throws Exception {
        // given
        // 회원가입을 통해 테스트할 멤버 생성
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "test12@example.com", "password123", "testUser12", "01012345678", "Test Address");
        ResultActions signUpResult = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        String responseBody = signUpResult.andReturn().getResponse().getContentAsString();
        long memberId = objectMapper.readTree(responseBody).get("data").get("memberId").asLong();

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}", memberId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("조회 성공"))
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.nickname").value("testUser12"));
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void t13() throws Exception {
        // given
        // 회원가입
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "withdraw@example.com", "password123", "withdrawUser", "01033334444", "Withdraw Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        // 로그인하여 토큰 발급
        LoginRequestDto loginDto = new LoginRequestDto("withdraw@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));
        String responseBody = loginResult.andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();

        // when
        // 회원탈퇴
        ResultActions withdrawResult = mockMvc.perform(delete("/api/v1/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // then
        withdrawResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-5"))
                .andExpect(jsonPath("$.msg").value("회원 탈퇴가 완료되었습니다."));

        // given
        // 탈퇴한 계정으로 다시 로그인 시도
        LoginRequestDto reloginDto = new LoginRequestDto("withdraw@example.com", "password123");

        // when
        ResultActions reloginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reloginDto)))
                .andDo(print());

        // then
        // 존재하지 않는 이메일이므로 404 Not Found 응답을 기대
        reloginResult.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 프로필 이미지 제외")
    void t14() throws Exception {
        // given: FileService가 항상 짧은 URL을 반환하도록 Mocking
        given(fileService.uploadFile(any(), any())).willReturn("https://test.com/initial.jpg");

        // 1. 초기 이미지와 함께 회원가입 및 정보 수정
        MemberSignUpRequestDto signUpDto = new MemberSignUpRequestDto(
                "image-test@example.com", "password123", "imageUser", "01055556666", "Image Address");
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpDto)));

        LoginRequestDto loginDto = new LoginRequestDto("image-test@example.com", "password123");
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));
        String accessToken = JsonPath.read(loginResult.andReturn().getResponse().getContentAsString(), "$.data.accessToken");

        MockMultipartFile initialImage = new MockMultipartFile("profileImage", "initial.jpg", MediaType.IMAGE_JPEG_VALUE, "initial image".getBytes());
        MemberModifyRequestDto initialModifyDto = new MemberModifyRequestDto("imageUser", "01055556666", "Image Address");
        MockMultipartFile initialModifyDtoPart = new MockMultipartFile("memberModifyRequestDto", "", "application/json", objectMapper.writeValueAsString(initialModifyDto).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/me")
                .file(initialImage)
                .file(initialModifyDtoPart)
                .header("Authorization", "Bearer " + accessToken));

        // 2. 이미지를 제외하고 다른 정보만 수정
        MemberModifyRequestDto secondModifyDto = new MemberModifyRequestDto("newNickname", "01077778888", "New Address");
        MockMultipartFile secondModifyDtoPart = new MockMultipartFile("memberModifyRequestDto", "", "application/json", objectMapper.writeValueAsString(secondModifyDto).getBytes(StandardCharsets.UTF_8));

        ResultActions secondModifyResult = mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/me")
                        .file(secondModifyDtoPart) // 이미지를 보내지 않음
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print());

        // 3. 닉네임은 변경되고, 이미지 URL은 1차 수정 때의 값("https://test.com/initial.jpg")이 그대로 유지되는지 확인
        secondModifyResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-4"))
                .andExpect(jsonPath("$.data.nickname").value("newNickname"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://test.com/initial.jpg"));
    }
}