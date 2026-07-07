package com.sparta.ordering.product.dto;

import com.sparta.ordering.product.entity.Product;

import java.util.UUID;

public record ProductResponseDto(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        Long price
) {

    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getRestaurant().getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }
}
