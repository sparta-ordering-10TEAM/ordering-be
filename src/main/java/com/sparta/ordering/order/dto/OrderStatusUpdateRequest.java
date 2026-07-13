package com.sparta.ordering.order.dto;

import com.sparta.ordering.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status
) {
}