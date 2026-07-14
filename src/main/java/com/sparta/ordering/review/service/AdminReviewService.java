package com.sparta.ordering.review.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.review.entity.Review;
import com.sparta.ordering.review.repository.ReviewRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final Set<Role> allowedRoles = new HashSet<>(List.of(Role.MANAGER, Role.MASTER));

    @Transactional
    public void softDeleteReview(UUID reviewId, UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!allowedRoles.contains(user.getRole())) { // 사용자 Role 검증
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        Review review = reviewRepository.findByIdAndDeletedAtIsNullWithOrder(reviewId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.REVIEW_NOT_FOUND));

        review.softDelete(userId);
        restaurantRepository.updateAverageRating(review.getOrder().getRestaurant().getId());
    }
}
