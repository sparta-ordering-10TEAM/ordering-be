package com.sparta.ordering.order.dto;

import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderStatus;

import java.util.UUID;

public record OrderStatusResponse(
        UUID orderId,
        OrderStatus status
) {
    public static OrderStatusResponse from(Order order) {
        return new OrderStatusResponse(
                order.getId(),
                order.getOrderStatus()
        );
    }
}
