package com.sparta.ordering.ai.facade;

import com.sparta.ordering.ai.client.GeminiClient;
import com.sparta.ordering.ai.entity.PromptType;
import com.sparta.ordering.ai.service.AiPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiPromptFacade {
    private final GeminiClient geminiClient;
    private final AiPromptService aiPromptService;

    public String generateProductDescription(String prompt) {
        String fullPrompt = PromptType.COMMON_GUARDRAILS
                + "[역할]\n" + PromptType.PRODUCT_DESC.getSystemInstruction() + "\n\n"
                + "[제약조건]\n" + PromptType.PRODUCT_DESC.getSpecificConstraints() + "\n\n"
                + "[사용자 요청]\n" + prompt;

        // 외부 API 호출 (트랜잭션 바깥)
        String generatedText = geminiClient.generateDescription(fullPrompt);

        // 데이터베이스 로그 적재 (짧은 트랜잭션)
        aiPromptService.savePromptLog(fullPrompt, generatedText, PromptType.PRODUCT_DESC);

        return generatedText;
    }
}
