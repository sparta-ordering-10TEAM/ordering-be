package com.sparta.ordering.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateProductDescriptionRequest(
        @NotBlank(message = "상품 설명 프롬프트를 입력해주세요.")
        @Size(max = 50, message = "프롬프트는 50자 이내로 작성해주세요.")
        String prompt
) {
}
