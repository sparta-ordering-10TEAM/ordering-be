package com.sparta.ordering.restaurant.controller;

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

    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @PostMapping("/restaurant-categories")
    public ResponseEntity<GeneralResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.CREATED,
                restaurantCategoryService.createCategory(request, userId)
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @PatchMapping("/restaurant-categories/{categoryId}")
    public ResponseEntity<GeneralResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                restaurantCategoryService.updateCategory(categoryId, request, userId)
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @DeleteMapping("/restaurant-categories/{categoryId}")
    public ResponseEntity<GeneralResponse<Void>> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UUID userId
    ) {
        restaurantCategoryService.deleteCategory(categoryId, userId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}
