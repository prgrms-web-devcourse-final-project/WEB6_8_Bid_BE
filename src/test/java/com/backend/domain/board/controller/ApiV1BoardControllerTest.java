package com.backend.domain.board.controller;

import com.backend.domain.board.constant.BoardType;
import com.backend.domain.board.dto.request.BoardWriteRequest;
import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.elasticsearch.TestElasticsearchConfiguration;
import com.backend.global.redis.TestRedisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test") // 테스트용 프로파일 활성화..
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
@Import({TestElasticsearchConfiguration.class, TestRedisConfiguration.class})
class ApiV1BoardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setEmail("test@test.com");
        testMember.setPassword("password");
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("게시글 작성 성공 - 통합 테스트")
    void createBoard_success() throws Exception {
        // given..
        BoardWriteRequest request = new BoardWriteRequest(
                "통합 테스트 제목",
                "통합 테스트 내용입니다.",
                BoardType.FAQ
        );

        // when..
        ResultActions resultActions = mvc
                .perform(post("/api/v1/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // then..
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("게시글이 작성되었습니다"))
                .andExpect(jsonPath("$.data.id").isNumber()) // ID가 생성되었는지 확인
                .andExpect(jsonPath("$.data.title").value("통합 테스트 제목"))
                .andExpect(jsonPath("$.data.content").value("통합 테스트 내용입니다."));
    }
}

