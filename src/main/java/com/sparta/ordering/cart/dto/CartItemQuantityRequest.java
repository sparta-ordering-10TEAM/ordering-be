package com.sparta.ordering.cart.dto;

import com.sparta.ordering.cart.CartPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemQuantityRequest (
        @Positive(message = "수량은 1개 이상이어야 합니다.")
        @NotNull(message = "수량은 필수입니다.")
        @Max(value = CartPolicy.MAX_QUANTITY, message = "수량은 99개를 초과할 수 없습니다.")
        Integer quantity

){
}
