package com.sparta.ordering.order.dto;

import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
import com.sparta.ordering.order.entity.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderListResponse(
        UUID orderId,
        UUID restaurantId,
        String restaurantName,
        Long totalPrice,
        OrderStatus status,
        List<OrderItemResponse> orderItems,
        Instant createdAt
) {
    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getTotalPrice(),
                order.getOrderStatus(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getCreatedAt()
        );
    }

    public record OrderItemResponse(
            String productName,
            int quantity
    ){
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductName(),
                    item.getQuantity()
            );
        }
    }
}