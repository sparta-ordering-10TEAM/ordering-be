package com.sparta.ordering.restaurant.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.dto.CategoryCreateRequest;
import com.sparta.ordering.restaurant.dto.CategoryResponse;
import com.sparta.ordering.restaurant.dto.CategoryUpdateRequest;
import com.sparta.ordering.restaurant.service.RestaurantCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RestaurantCategoryController {

    private final RestaurantCategoryService restaurantCategoryService;

    @GetMapping("/restaurant-categories")
    public ResponseEntity<GeneralResponse<List<CategoryResponse>>> getCategories() {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantCategoryService.getCategories()
        );
    }

    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    @PostMapping("/restaurant-categories")
    public ResponseEntity<GeneralResponse<CategoryResponse>> postCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantCategoryService.createCategory(request, user.getUserId())
        );
    }

    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    @PatchMapping("/restaurant-categories/{categoryId}")
    public ResponseEntity<GeneralResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
            ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantCategoryService.updateCategory(categoryId, request, user.getUserId())
        );
    }

    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    @DeleteMapping("/restaurant-categories/{categoryId}")
    public ResponseEntity<GeneralResponse<Void>> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        restaurantCategoryService.deleteCategory(categoryId, user.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

}
