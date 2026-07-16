package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.config.CacheConfig;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.CategoryCreateRequest;
import com.sparta.ordering.restaurant.dto.CategoryResponse;
import com.sparta.ordering.restaurant.dto.CategoryUpdateRequest;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.repository.RestaurantCategoryRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantCategoryService {

    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Cacheable(CacheConfig.CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return restaurantCategoryRepository.findByDeletedAtIsNull().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request, UUID userId) {
        validateAdminPermission(userId);

        if (restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull(request.code())) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_ALREADY_EXISTS);
        }

        RestaurantCategory category = RestaurantCategory.builder()
                .code(request.code())
                .build();

        return CategoryResponse.from(restaurantCategoryRepository.save(category));
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, CategoryUpdateRequest request, UUID userId) {
        validateAdminPermission(userId);

        RestaurantCategory category = getActiveCategory(categoryId);

        if (!category.getCode().equals(request.code())
                && restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull(request.code())) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_ALREADY_EXISTS);
        }

        category.updateCode(request.code());

        return CategoryResponse.from(category);
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public void deleteCategory(UUID categoryId, UUID userId) {
        validateAdminPermission(userId);

        RestaurantCategory category = getActiveCategory(categoryId);

        if (restaurantRepository.existsByCategory_IdAndDeletedAtIsNull(categoryId)) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_IN_USE);
        }

        category.softDelete(userId);
    }

    private void validateAdminPermission(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!user.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }
    }

    private RestaurantCategory getActiveCategory(UUID categoryId) {
        return restaurantCategoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND));
    }
}
