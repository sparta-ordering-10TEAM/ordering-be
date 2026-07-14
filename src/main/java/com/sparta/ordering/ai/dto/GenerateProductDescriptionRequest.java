package com.sparta.ordering.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateProductDescriptionRequest(
        @NotBlank(message = "상품 설명 프롬프트를 입력해주세요.")
        String prompt
) {
}
