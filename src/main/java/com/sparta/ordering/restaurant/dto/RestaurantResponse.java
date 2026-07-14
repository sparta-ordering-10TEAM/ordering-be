package com.sparta.ordering.restaurant.dto;

import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record RestaurantResponse(
        UUID restaurantId,
        UUID ownerUserId,
        String category,
        UUID regionId,
        String regionName,
        String name,
        String phone,
        String description,
        String address,
        String addressDetail,
        String zipCode,
        Integer minOrderAmount,
        Integer deliveryFee,
        RestaurantStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal deliveryRadiusKm,
        Double averageRating,
        Long reviewCount
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getUser().getId(),
                restaurant.getCategory().getCode(),
                restaurant.getRegion().getId(),
                restaurant.getRegion().getName(),
                restaurant.getName(),
                restaurant.getPhone(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getAddressDetail(),
                restaurant.getZipCode(),
                restaurant.getMinOrderAmount(),
                restaurant.getDeliveryFee(),
                restaurant.getStatus(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getDeliveryRadiusKm(),
                restaurant.getAverageRating(),
                restaurant.getReviewCount()
        );
    }
}
