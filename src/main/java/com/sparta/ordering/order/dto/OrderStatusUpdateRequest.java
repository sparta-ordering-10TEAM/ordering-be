package com.sparta.ordering.order.dto;

import com.sparta.ordering.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "변경할 주문 상태는 필수입니다.")
        OrderStatus status
) {
}