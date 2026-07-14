package com.sparta.ordering.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.ai.client.GeminiClient;
import com.sparta.ordering.ai.entity.AiPromptLog;
import com.sparta.ordering.ai.entity.PromptType;
import com.sparta.ordering.ai.repository.AiPromptLogRepository;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AiPromptControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiPromptLogRepository aiPromptLogRepository;

    @Autowired
    private JwtSessionService jwtSessionService;

    @MockitoBean
    private GeminiClient geminiClient;

    private User owner;
    private User manager;
    private User customer;

    private String ownerToken;
    private String managerToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        owner = User.builder()
                .userName("owner_" + UUID.randomUUID())
                .nickName("owner_nick_" + UUID.randomUUID())
                .email("owner_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-1234-5678")
                .role(Role.OWNER)
                .password("password")
                .build();
        userRepository.save(owner);

        manager = User.builder()
                .userName("manager_" + UUID.randomUUID())
                .nickName("manager_nick_" + UUID.randomUUID())
                .email("manager_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-9876-5432")
                .role(Role.MANAGER)
                .password("password")
                .build();
        userRepository.save(manager);

        customer = User.builder()
                .userName("customer_" + UUID.randomUUID())
                .nickName("customer_nick_" + UUID.randomUUID())
                .email("customer_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-5555-5555")
                .role(Role.CUSTOMER)
                .password("password")
                .build();
        userRepository.save(customer);

        ownerToken = jwtSessionService.createJwtSession(owner.getId()).getAccessToken();
        managerToken = jwtSessionService.createJwtSession(manager.getId()).getAccessToken();
        customerToken = jwtSessionService.createJwtSession(customer.getId()).getAccessToken();

        // GeminiClient 기본 동작 모킹
        when(geminiClient.generateDescription(anyString())).thenReturn("바삭바삭 매운 떡볶이 설명문구");
    }

    @Nested
    @DisplayName("AI 상품 설명 생성 (POST /api/products/generate-description)")
    class GenerateProductDescription {

        @Test
        @DisplayName("성공 - OWNER 권한으로 호출 시 AI 생성 내용 반환 및 로그 적재 확인")
        void success() throws Exception {
            // given
            String requestBody = "{\"prompt\": \"매운 떡볶이 설명 생성해줘\"}";

            // when & then
            mockMvc.perform(post("/api/products/generate-description")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data").value("바삭바삭 매운 떡볶이 설명문구"));

            // DB 로그 확인
            long logCount = aiPromptLogRepository.count();
            assertThat(logCount).isEqualTo(1);
        }

        @Test
        @DisplayName("실패 - CUSTOMER 권한으로 호출 시 403 Forbidden 발생")
        void failForbidden() throws Exception {
            // given
            String requestBody = "{\"prompt\": \"매운 떡볶이 설명 생성해줘\"}";

            // when & then
            mockMvc.perform(post("/api/products/generate-description")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 - 프롬프트 글자 수가 50자를 초과하는 경우 400 Bad Request 발생")
        void failPromptSizeExceeded() throws Exception {
            // given
            String requestBody = "{\"prompt\": \"" + "A".repeat(51) + "\"}";

            // when & then
            mockMvc.perform(post("/api/products/generate-description")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("유효하지 않은 요청입니다."));
        }
    }

    @Nested
    @DisplayName("관리자 AI 프롬프트 로그 관리")
    class AdminLogManagement {

        @Test
        @DisplayName("성공 - MANAGER 권한으로 전체 로그 목록 조회")
        void searchSuccess() throws Exception {
            // given
            AiPromptLog log1 = AiPromptLog.builder()
                    .prompt("프롬프트1")
                    .responseText("응답1")
                    .promptType(PromptType.PRODUCT_DESC)
                    .build();
            aiPromptLogRepository.save(log1);

            // when & then
            mockMvc.perform(get("/api/admin/ai-prompts")
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content[0].prompt").value("프롬프트1"));
        }

        @Test
        @DisplayName("성공 - MANAGER 권한으로 로그 단건 상세 조회")
        void getLogSuccess() throws Exception {
            // given
            AiPromptLog log1 = AiPromptLog.builder()
                    .prompt("상세프롬프트")
                    .responseText("상세응답")
                    .promptType(PromptType.PRODUCT_DESC)
                    .build();
            aiPromptLogRepository.save(log1);

            // when & then
            mockMvc.perform(get("/api/admin/ai-prompts/{logId}", log1.getId())
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.prompt").value("상세프롬프트"))
                    .andExpect(jsonPath("$.data.responseText").value("상세응답"));
        }

        @Test
        @DisplayName("성공 - MANAGER 권한으로 로그 삭제(소프트딜리트) 확인")
        void deleteSuccess() throws Exception {
            // given
            AiPromptLog log1 = AiPromptLog.builder()
                    .prompt("삭제프롬프트")
                    .responseText("삭제응답")
                    .promptType(PromptType.PRODUCT_DESC)
                    .build();
            aiPromptLogRepository.save(log1);

            // when & then
            mockMvc.perform(delete("/api/admin/ai-prompts/{logId}", log1.getId())
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // DB에서 deletedAt 검증
            AiPromptLog deleted = aiPromptLogRepository.findById(log1.getId()).orElseThrow();
            assertThat(deleted.getDeletedAt()).isNotNull();
            assertThat(deleted.getDeletedBy()).isEqualTo(manager.getId());
        }
    }
}
