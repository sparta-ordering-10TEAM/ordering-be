package com.sparta.ordering.order.dto;

import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
import com.sparta.ordering.order.entity.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderNumber,
        UUID restaurantId,
        String restaurantName,
        String deliveryAddress,
        String requestMessage,
        Long totalPrice,
        OrderStatus status,
        List<OrderItemDetailResponse> orderItems,
        Instant createdAt
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getDeliveryAddress(),
                order.getRequestMessage(),
                order.getTotalPrice(),
                order.getOrderStatus(),
                order.getOrderItems().stream()
                        .map(OrderItemDetailResponse::from)
                        .toList(),
                order.getCreatedAt()
        );
    }

    public record OrderItemDetailResponse(
            String productName,
            int quantity,
            Long productPrice,
            Long totalPrice
    ){
        public static OrderItemDetailResponse from(OrderItem item) {
            return new OrderItemDetailResponse(
                    item.getProductName(),
                    item.getQuantity(),
                    item.getProductPrice(),
                    item.getTotalPrice()
            );
        }
    }
}