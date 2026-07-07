package com.sparta.ordering.aipromptlog.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateAiProductDescriptionRequest(
        @NotBlank(message = "프롬프트는 필수 입력값입니다.")
        String prompt
) {
}
