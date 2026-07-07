package com.sparta.ordering.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductCreateRequest(

        @NotNull(message = "가게 ID는 필수입니다.")
        UUID restaurantId,

        @NotBlank(message = "상품 명은 필수 입니다.")
        @Size(max = 100, message = "상품명은 100자를 넘을 수 없습니다.")
        String name,

        String description,

        @NotNull(message = "가격은 필수입니다.")
        @Positive(message = "가격은 0보다 커야 합니다.")
        Long price
) {
}
