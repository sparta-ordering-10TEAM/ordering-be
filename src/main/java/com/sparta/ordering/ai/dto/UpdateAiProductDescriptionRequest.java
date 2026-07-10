package com.sparta.ordering.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAiProductDescriptionRequest(
        @NotBlank(message = "상품 설명은 필수 입력값입니다.")
        String description
) {
}
