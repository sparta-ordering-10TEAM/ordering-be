package com.sparta.ordering.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank
        @Size(min = 1, max = 20, message = "카테고리 코드는 1자 이상 20자 이하여야 합니다.")
        String code
) {
}
