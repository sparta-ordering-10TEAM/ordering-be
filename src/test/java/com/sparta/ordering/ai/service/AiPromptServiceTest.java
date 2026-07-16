package com.sparta.ordering.ai.service;

import com.sparta.ordering.ai.client.GeminiClient;
import com.sparta.ordering.ai.dto.AiPromptLogResponse;
import com.sparta.ordering.ai.entity.AiPromptLog;
import com.sparta.ordering.ai.entity.PromptType;
import com.sparta.ordering.ai.facade.AiPromptFacade;
import com.sparta.ordering.ai.repository.AiPromptLogRepository;
import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.review.dto.ReviewDetailResponse;
import com.sparta.ordering.review.service.ReviewService;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiPromptServiceTest {

    @Mock
    private AiPromptLogRepository aiPromptLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private AiPromptService aiPromptService;

    @InjectMocks
    private AdminAiPromptService adminAiPromptService;

    @Nested
    @DisplayName("AI 프롬프트 저장 (savePromptLog)")
    class SavePromptLog {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            String prompt = "테스트 프롬프트";
            String responseText = "테스트 결과";
            PromptType promptType = PromptType.PRODUCT_DESC;

            // when
            aiPromptService.savePromptLog(prompt, responseText, promptType);

            // then
            verify(aiPromptLogRepository, times(1)).save(any(AiPromptLog.class));
        }
    }

    @Nested
    @DisplayName("AI 프롬프트 파사드 생성 (generateProductDescription)")
    class GenerateProductDescription {

        @Test
        @DisplayName("성공 - 50자 이하인 경우 생략 처리되지 않음")
        void success() {
            // given
            String prompt = "치킨 설명 생성";
            String generatedText = "바삭하고 고소한 후라이드 치킨";
            when(geminiClient.generateText(anyString(), anyString())).thenReturn(generatedText);

            AiPromptFacade facade = new AiPromptFacade(geminiClient, aiPromptService, reviewService);

            // when
            String result = facade.generateProductDescription(prompt);

            // then
            assertThat(result).isEqualTo(generatedText);
            verify(geminiClient, times(1)).generateText(anyString(), eq(prompt));
            verify(aiPromptLogRepository, times(1)).save(any(AiPromptLog.class));
        }

        @Test
        @DisplayName("성공 - 50자를 초과하는 경우 47자 자른 뒤 ...을 붙여 50자로 만듦")
        void successWithTruncate() {
            // given
            String prompt = "치킨 설명 생성";
            String generatedText = "A".repeat(60);
            when(geminiClient.generateText(anyString(), anyString())).thenReturn(generatedText);

            AiPromptFacade facade = new AiPromptFacade(geminiClient, aiPromptService, reviewService);

            // when
            String result = facade.generateProductDescription(prompt);

            // then
            String expected = "A".repeat(47) + "...";
            assertThat(result).isEqualTo(expected);
            assertThat(result.length()).isEqualTo(50);
            verify(geminiClient, times(1)).generateText(anyString(), eq(prompt));
            verify(aiPromptLogRepository, times(1)).save(any(AiPromptLog.class));
        }

        @Test
        @DisplayName("성공 - 정확히 50자인 경우 생략 처리되지 않음")
        void successWithExactLimit() {
            // given
            String prompt = "치킨 설명 생성";
            String generatedText = "A".repeat(50);
            when(geminiClient.generateText(anyString(), anyString())).thenReturn(generatedText);

            AiPromptFacade facade = new AiPromptFacade(geminiClient, aiPromptService, reviewService);

            // when
            String result = facade.generateProductDescription(prompt);

            // then
            assertThat(result).isEqualTo(generatedText);
            assertThat(result.length()).isEqualTo(50);
            verify(geminiClient, times(1)).generateText(anyString(), eq(prompt));
            verify(aiPromptLogRepository, times(1)).save(any(AiPromptLog.class));
        }
    }

    @Nested
    @DisplayName("AI 프롬프트 파사드 리뷰 답변 생성 (generateReviewReply)")
    class GenerateReviewReply {

        @Test
        @DisplayName("성공 - 200자 이하인 경우 생략 처리되지 않음")
        void success() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String reviewComment = "맛있어요!";
            String expectedText = "리뷰를 남겨주셔서 감사합니다. 맛있게 드셔주셨다니 기쁩니다!";

            ReviewDetailResponse review = new ReviewDetailResponse(
                    reviewId, UUID.randomUUID(), UUID.randomUUID(), 5, reviewComment,
                    java.time.Instant.now(), java.time.Instant.now(), userId, userId, null
            );

            when(reviewService.getReview(reviewId)).thenReturn(review);
            when(geminiClient.generateText(anyString(), eq(reviewComment))).thenReturn(expectedText);

            AiPromptFacade facade = new AiPromptFacade(geminiClient, aiPromptService, reviewService);

            // when
            String result = facade.generateReviewReply(reviewId, userId);

            // then
            assertThat(result).isEqualTo(expectedText);
            verify(geminiClient, times(1)).generateText(anyString(), eq(reviewComment));
            verify(aiPromptLogRepository, times(1)).save(any(AiPromptLog.class));
        }

        @Test
        @DisplayName("성공 - 200자를 초과하는 경우 197자 자른 뒤 ...을 붙여 200자로 만듦")
        void successWithTruncate() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String reviewComment = "맛있어요!";
            String generatedText = "A".repeat(250);

            ReviewDetailResponse review = new ReviewDetailResponse(
                    reviewId, UUID.randomUUID(), UUID.randomUUID(), 5, reviewComment,
                    java.time.Instant.now(), java.time.Instant.now(), userId, userId, null
            );

            when(reviewService.getReview(reviewId)).thenReturn(review);
            when(geminiClient.generateText(anyString(), eq(reviewComment))).thenReturn(generatedText);

            AiPromptFacade facade = new AiPromptFacade(geminiClient, aiPromptService, reviewService);

            // when
            String result = facade.generateReviewReply(reviewId, userId);

            // then
            String expected = "A".repeat(197) + "...";
            assertThat(result).isEqualTo(expected);
            assertThat(result.length()).isEqualTo(200);
            verify(geminiClient, times(1)).generateText(anyString(), eq(reviewComment));
            verify(aiPromptLogRepository, times(1)).save(any(AiPromptLog.class));
        }
    }

    @Nested
    @DisplayName("관리자 AI 프롬프트 서비스")
    class AdminPromptServiceTest {

        @Test
        @DisplayName("로그 삭제 성공")
        void deleteSuccess() {
            // given
            UUID logId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MANAGER);

            AiPromptLog log = spy(AiPromptLog.builder()
                    .prompt("프롬프트")
                    .responseText("응답")
                    .promptType(PromptType.PRODUCT_DESC)
                    .build());

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(aiPromptLogRepository.findByIdAndDeletedAtIsNull(logId)).thenReturn(Optional.of(log));

            // when
            adminAiPromptService.delete(logId, adminId);

            // then
            verify(log, times(1)).softDelete(adminId);
            assertThat(log.getDeletedAt()).isNotNull();
            assertThat(log.getDeletedBy()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("로그 삭제 실패 - 권한 없음")
        void deleteForbidden() {
            // given
            UUID logId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User customer = mock(User.class);
            when(customer.getRole()).thenReturn(Role.CUSTOMER);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(customer));

            // when & then
            assertThatThrownBy(() -> adminAiPromptService.delete(logId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(aiPromptLogRepository);
        }

        @Test
        @DisplayName("로그 목록 조회 성공")
        void searchSuccess() {
            // given
            UUID adminId = UUID.randomUUID();
            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MASTER);

            AiPromptLog log = AiPromptLog.builder()
                    .prompt("프롬프트")
                    .responseText("응답")
                    .promptType(PromptType.PRODUCT_DESC)
                    .build();
            ReflectionTestUtils.setField(log, "id", UUID.randomUUID());

            Pageable pageable = PageRequest.of(0, 10);
            Page<AiPromptLog> logPage = new PageImpl<>(List.of(log), pageable, 1);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(aiPromptLogRepository.findByDeletedAtIsNull(pageable)).thenReturn(logPage);

            // when
            Page<AiPromptLogResponse> result = adminAiPromptService.search(adminId, pageable);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).prompt()).isEqualTo("프롬프트");
            verify(aiPromptLogRepository, times(1)).findByDeletedAtIsNull(pageable);
        }

        @Test
        @DisplayName("로그 단건 조회 성공")
        void getLogSuccess() {
            // given
            UUID logId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MASTER);

            AiPromptLog log = AiPromptLog.builder()
                    .prompt("프롬프트")
                    .responseText("응답")
                    .promptType(PromptType.PRODUCT_DESC)
                    .build();
            ReflectionTestUtils.setField(log, "id", logId);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(aiPromptLogRepository.findByIdAndDeletedAtIsNull(logId)).thenReturn(Optional.of(log));

            // when
            AiPromptLogResponse result = adminAiPromptService.getPromptLog(logId, adminId);

            // then
            assertThat(result.id()).isEqualTo(logId);
            assertThat(result.prompt()).isEqualTo("프롬프트");
        }
    }
}
