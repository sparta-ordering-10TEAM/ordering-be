package com.sparta.ordering.product.dto;

import com.sparta.ordering.product.entity.Product;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        Long price
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getRestaurant().getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }
}
