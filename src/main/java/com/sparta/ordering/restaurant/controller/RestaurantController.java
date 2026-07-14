package com.sparta.ordering.restaurant.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.controller.api.RestaurantApi;
import com.sparta.ordering.restaurant.dto.RestaurantCreateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.dto.RestaurantStatusUpdateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantUpdateRequest;
import com.sparta.ordering.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
public class RestaurantController implements RestaurantApi {

    private final RestaurantService restaurantService;

    @Override
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

    @Override
    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<GeneralResponse<RestaurantResponse>> getRestaurant(@PathVariable UUID restaurantId) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.getRestaurant(restaurantId)
        );
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/users/me/restaurants")
    public ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> getOwnerRestaurants(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault Pageable pageable
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.getOwnerRestaurants(user.getUserId(), pageable)
        );
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/restaurants")
    public ResponseEntity<GeneralResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody RestaurantCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.CREATED,
                restaurantService.createRestaurant(request, user.getUserId())
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER', 'OWNER')")
    @PatchMapping("/restaurants/{restaurantId}")
    public ResponseEntity<GeneralResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.updateRestaurant(restaurantId, request, user.getUserId())
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER', 'OWNER')")
    @PatchMapping("/restaurants/{restaurantId}/status")
    public ResponseEntity<GeneralResponse<RestaurantResponse>> changeRestaurantStatus(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantService.changeRestaurantStatus(restaurantId, request.status(), user.getUserId())
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER', 'OWNER')")
    @DeleteMapping("/restaurants/{restaurantId}")
    public ResponseEntity<GeneralResponse<Void>> deleteRestaurant(
            @PathVariable UUID restaurantId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        restaurantService.deleteRestaurant(restaurantId, user.getUserId());

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}
