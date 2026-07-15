package com.sparta.ordering.ai.dto;

import com.sparta.ordering.ai.entity.AiPromptLog;
import com.sparta.ordering.ai.entity.PromptType;

import java.time.Instant;
import java.util.UUID;

public record AiPromptLogResponse(
        UUID id,
        String prompt,
        String responseText,
        PromptType promptType,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {
    public static AiPromptLogResponse fromEntity(AiPromptLog log) {
        return new AiPromptLogResponse(
                log.getId(),
                log.getPrompt(),
                log.getResponseText(),
                log.getPromptType(),
                log.getCreatedAt(),
                log.getUpdatedAt(),
                log.getCreatedBy(),
                log.getUpdatedBy()
        );
    }
}
