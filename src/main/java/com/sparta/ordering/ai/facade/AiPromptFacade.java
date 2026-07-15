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
        int maxLength = 50;
        String systemInstruction = buildSystemInstruction(PromptType.PRODUCT_DESC);

        // 외부 API 호출 (트랜잭션 바깥)
        String generatedText = geminiClient.generateDescription(systemInstruction, prompt);
        if (generatedText.length() > maxLength) { // 글자 수 50글자로 조정
            generatedText = generatedText.substring(0, maxLength - 3) + "...";
        }

        // 데이터베이스 로그 적재 (짧은 트랜잭션)
        String fullPrompt = systemInstruction + "\n\n[사용자 요청]\n" + prompt;
        aiPromptService.savePromptLog(fullPrompt, generatedText, PromptType.PRODUCT_DESC);

        return generatedText;
    }

    private String buildSystemInstruction(PromptType promptType) {
        return PromptType.COMMON_GUARDRAILS
                + "[역할]\n" + promptType.getSystemInstruction() + "\n\n"
                + "[제약조건]\n" + promptType.getSpecificConstraints();
    }
}
