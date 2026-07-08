package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.repository.RestaurantCategoryRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getRestaurants(String category, Pageable pageable) {
        return findRestaurants(category, pageable)
                .map(RestaurantResponse::from);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(UUID restaurantId) {
        Restaurant restaurant = getActiveRestaurant(restaurantId);

        return RestaurantResponse.from(restaurant);
    }

    private Page<Restaurant> findRestaurants(String category, Pageable pageable) {
        if (StringUtils.hasText(category)) {
            RestaurantCategory restaurantCategory = getActiveCategory(category);

            return restaurantRepository.findByCategoryAndDeletedAtIsNull(restaurantCategory, pageable);
        }

        return restaurantRepository.findByDeletedAtIsNull(pageable);
    }

    private RestaurantCategory getActiveCategory(String category) {
        return restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(category)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND));
    }

    private Restaurant getActiveRestaurant(UUID restaurantId) {
        return restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND));
    }
}
