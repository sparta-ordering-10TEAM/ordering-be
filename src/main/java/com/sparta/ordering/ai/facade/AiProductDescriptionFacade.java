package com.sparta.ordering.ai.facade;

import com.sparta.ordering.ai.service.AiProductDescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiProductDescriptionFacade {
    private final AiProductDescriptionService aiProductDescriptionService;

    private static final String PROMPT_CONSTRAINT = " (공백 포함 50자 이하로 생성해줘)";

    public void generate(UUID productId, UUID userId, String prompt) {
        // 유효성 검증 및 상품 조회 (짧은 Read 트랜잭션)
        aiProductDescriptionService.validateProduct(productId, userId);

        // 외부 API 호출 (트랜잭션 범위 밖 - DB 커넥션 미점유)
        String refinedPrompt = prompt + PROMPT_CONSTRAINT;
        String description = aiProductDescriptionService.generateDescription(refinedPrompt);

        // AI 상품 설명 저장 (짧은 Write 트랜잭션)
        aiProductDescriptionService.saveDescription(productId, refinedPrompt, description);
    }
}
