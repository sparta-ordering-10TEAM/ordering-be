package com.sparta.ordering.review.service;

import com.sparta.ordering.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    public void postReview(int rating, String comment, UUID orderId, UUID userId) {
    }

    public Page<ReviewResponse> searchRestaurantReviews(UUID restaurantId, Pageable pageable) {
        return null;
    }

    public Page<ReviewResponse> searchProductReviews(UUID productId, Pageable pageable) {
        return null;
    }

    public Double getRestaurantAverageRating(UUID restaurantId) {
        return null;
    }

    public void updateReview(UUID reviewId, UUID userId) {
    }

    public void softDeleteReview(UUID reviewId, UUID userId) {
    }
}
