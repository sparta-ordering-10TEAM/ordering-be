package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RestaurantService {

    public Page<RestaurantResponse> getRestaurants(RestaurantCategory category, Pageable pageable) {
        return null;
    }

    public RestaurantResponse getRestaurant(UUID restaurantId) {
        return null;
    }
}
