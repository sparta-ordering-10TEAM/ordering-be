package com.sparta.ordering.cart.dto;

import com.sparta.ordering.cart.entity.CartItem;

import java.util.UUID;

public record CartItemResponse(
        UUID cartItemId,
        UUID productId,
        String productName,
        Long price,
        int quantity
) {

    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity()
        );
    }
}