package com.sparta.ordering.product.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @Size(max = 100, message = "상품명은 100자를 넘을 수 없습니다.")
        String name,
        String description,

        @Positive(message = "가격은 0보다 커야 합니다.")
        Long price
) {
}
