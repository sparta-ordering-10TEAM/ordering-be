package com.sparta.ordering.restaurant.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.dto.RestaurantCreateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/restaurants")
    public ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> getRestaurants(
            @RequestParam(required = false) String category,
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

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/users/me/restaurants")
    public ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> getOwnerRestaurants(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault Pageable pageable
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.getOwnerRestaurants(userId, pageable)
        );
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/restaurants")
    public ResponseEntity<GeneralResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody RestaurantCreateRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.CREATED,
                restaurantService.createRestaurant(request, userId)
        );
    }
}
