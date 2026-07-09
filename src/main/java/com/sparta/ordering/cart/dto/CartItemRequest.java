package com.sparta.ordering.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CartItemRequest (
        @NotNull(message = "상품 ID는 필수입니다.")
        UUID productId,

        @Positive(message = "수량은 1개 이상이어야 합니다.")
        int quantity
) {
}
