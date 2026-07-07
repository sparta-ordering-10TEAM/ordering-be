package com.sparta.ordering.review.dto;

import com.sparta.ordering.review.entity.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {
    public static ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getOrder().getId(),
                review.getCustomer().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getCreatedBy(),
                review.getUpdatedBy()
        );
    }
}
