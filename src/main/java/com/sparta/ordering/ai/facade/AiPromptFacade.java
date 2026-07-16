package com.sparta.ordering.ai.facade;

import com.sparta.ordering.ai.client.GeminiClient;
import com.sparta.ordering.ai.entity.PromptType;
import com.sparta.ordering.ai.service.AiPromptService;
import com.sparta.ordering.review.dto.ReviewDetailResponse;
import com.sparta.ordering.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiPromptFacade {
    private final GeminiClient geminiClient;
    private final AiPromptService aiPromptService;
    private final ReviewService reviewService;

    public String generateProductDescription(String prompt) {
        int maxLength = 50;
        String systemInstruction = buildSystemInstruction(PromptType.PRODUCT_DESC);

        // 외부 API 호출 (트랜잭션 바깥)
        String generatedText = geminiClient.generateText(systemInstruction, prompt);
        if (generatedText.length() > maxLength) { // 글자 수 50글자로 조정
            generatedText = generatedText.substring(0, maxLength - 3) + "...";
        }

        // 데이터베이스 로그 적재 (짧은 트랜잭션)
        String fullPrompt = systemInstruction + "\n\n[사용자 요청]\n" + prompt;
        aiPromptService.savePromptLog(fullPrompt, generatedText, PromptType.PRODUCT_DESC);

        return generatedText;
    }

    public String generateReviewReply(UUID reviewId, UUID userId) {
        int maxLength = 200;
        String systemInstruction = buildSystemInstruction(PromptType.REVIEW_REPLY);

        ReviewDetailResponse review = reviewService.getReview(reviewId);

        // 외부 API 호출 (트랜잭션 바깥)
        String generatedText = geminiClient.generateText(systemInstruction, review.comment());
        if (generatedText.length() > maxLength) { // 글자 수 200글자로 조정
            generatedText = generatedText.substring(0, maxLength - 3) + "...";
        }

        // 데이터베이스 로그 적재 (짧은 트랜잭션)
        String fullPrompt = systemInstruction + "\n\n[사용자 요청]\n" + review.comment();
        aiPromptService.savePromptLog(fullPrompt, generatedText, PromptType.REVIEW_REPLY);

        return generatedText;
    }

    private String buildSystemInstruction(PromptType promptType) {
        return PromptType.COMMON_GUARDRAILS
                + "[역할]\n" + promptType.getSystemInstruction() + "\n\n"
                + "[제약조건]\n" + promptType.getSpecificConstraints();
    }
}
