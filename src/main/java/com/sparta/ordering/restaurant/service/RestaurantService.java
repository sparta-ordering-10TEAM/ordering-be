package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.RestaurantCreateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.dto.RestaurantUpdateRequest;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import com.sparta.ordering.restaurant.repository.RestaurantCategoryRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final UserRepository userRepository;

    private static final Set<Role> PRIVILEGED_ROLES = Set.of(Role.MANAGER, Role.MASTER);

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

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getOwnerRestaurants(UUID userId, Pageable pageable) {
        return restaurantRepository.findByUser_IdAndDeletedAtIsNull(userId, pageable)
                .map(RestaurantResponse::from);
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantCreateRequest request, UUID userId) {
        User owner = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (owner.getRole() != Role.OWNER) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        RestaurantCategory category = restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(request.category())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND));

        Restaurant restaurant = Restaurant.builder()
                .user(owner)
                .category(category)
                .name(request.name())
                .description(request.description())
                .phone(request.phone())
                .address(request.address())
                .addressDetail(request.addressDetail())
                .zipCode(request.zipCode())
                .minOrderAmount(request.minOrderAmount())
                .deliveryFee(request.deliveryFee())
                .status(RestaurantStatus.CLOSED)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .deliveryRadiusKm(request.deliveryRadiusKm())
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return RestaurantResponse.from(savedRestaurant);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(
            UUID restaurantId,
            RestaurantUpdateRequest request,
            UUID userId
    ) {
        Restaurant restaurant = getActiveRestaurant(restaurantId);
        User requestUser = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        validateUpdatePermission(requestUser, restaurant);

        RestaurantCategory category = (request.category() != null) ? getActiveCategory(request.category()): null;

        restaurant.update(
                category,
                request.name(),
                request.phone(),
                request.description(),
                request.address(),
                request.addressDetail(),
                request.zipCode(),
                request.minOrderAmount(),
                request.deliveryFee(),
                request.latitude(),
                request.longitude(),
                request.deliveryRadiusKm()
        );

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

    private void validateUpdatePermission(User user, Restaurant restaurant) {
        if (PRIVILEGED_ROLES.contains(user.getRole())) {
            return;
        }
        if (user.getRole() == Role.OWNER && restaurant.isOwnedBy(user)) {
            return;
        }
        throw new ApiException(AuthResponseCode.FORBIDDEN);
    }
}
