package com.sparta.ordering.order.dto;

import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderStatus;

import java.util.UUID;

public record OrderCreateResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        Long totalPrice
) {
    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalPrice()
        );
    }
}