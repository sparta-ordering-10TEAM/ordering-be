package com.sparta.ordering.restaurant.dto;

import com.sparta.ordering.restaurant.entity.RestaurantCategory;

import java.util.UUID;

public record CategoryResponse(
        UUID categoryId,
        String code
) {
    public static CategoryResponse from(RestaurantCategory category) {
        return new CategoryResponse(category.getId(), category.getCode());
    }
}
