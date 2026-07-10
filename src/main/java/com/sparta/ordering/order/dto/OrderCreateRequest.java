package com.sparta.ordering.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull(message = "주문 식당은 필수입니다.")
        UUID restaurantId,

        @NotBlank(message = "배송지는 필수입니다.")
        @Size(max = 255)
        String deliveryAddress,

        @Size(max = 255)
        String requestMessage,

        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        List<@Valid OrderItemRequest> orderItems
) {
    public record OrderItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            UUID productId,

            @NotNull(message = "주문 수량은 필수입니다.")
            @Positive(message = "주문 수량은 1개 이상이어야 합니다.")
            Integer quantity
    ) {
    }
}