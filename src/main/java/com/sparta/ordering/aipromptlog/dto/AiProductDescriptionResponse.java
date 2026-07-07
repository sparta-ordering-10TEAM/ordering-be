package com.sparta.ordering.aipromptlog.dto;

import com.sparta.ordering.aipromptlog.entity.AiProductDescription;

import java.time.Instant;
import java.util.UUID;

public record AiProductDescriptionResponse(
        UUID id,
        UUID productId,
        String prompt,
        String description,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {
    public static AiProductDescriptionResponse fromEntity(AiProductDescription aiProductDescription) {
        return new AiProductDescriptionResponse(
                aiProductDescription.getId(),
                aiProductDescription.getProduct().getId(),
                aiProductDescription.getPrompt(),
                aiProductDescription.getDescription(),
                aiProductDescription.getCreatedAt(),
                aiProductDescription.getUpdatedAt(),
                aiProductDescription.getCreatedBy(),
                aiProductDescription.getUpdatedBy()
        );
    }
}
