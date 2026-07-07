package com.sparta.ordering.restaurant.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/restaurants")
    public ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> getRestaurants(
            @RequestParam(required = false) RestaurantCategory category,
            @PageableDefault Pageable pageable
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.getRestaurants(category, pageable)
        );
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<GeneralResponse<RestaurantResponse>> getRestaurant(@PathVariable UUID restaurantId) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.getRestaurant(restaurantId)
        );
    }
}
