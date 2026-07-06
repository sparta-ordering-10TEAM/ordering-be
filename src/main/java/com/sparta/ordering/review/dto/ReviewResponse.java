package com.sparta.ordering.review.dto;

import com.sparta.ordering.review.entity.Review;

import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        int rating,
        String comment
) {
    public ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getOrder().getId(),
                review.getCustomer().getId(),
                review.getRating(),
                review.getComment()
        );
    }
}
