package com.sparta.ordering.cart.dto;

import com.sparta.ordering.cart.entity.Cart;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID restaurantId,
        List<CartItemResponse> items
) {

    public static CartResponse from(Cart cart, List<CartItemResponse> items) {
        UUID restaurantId = cart.getRestaurant() != null ? cart.getRestaurant().getId() : null;
        return new CartResponse(cart.getId(), restaurantId, items);
    }

    public static CartResponse empty() {
        return new CartResponse(null, null, List.of());
    }
}
