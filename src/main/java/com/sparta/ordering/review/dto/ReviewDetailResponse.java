package com.sparta.ordering.review.dto;

import com.sparta.ordering.review.entity.Review;
import com.sparta.ordering.review.entity.ReviewReply;

import java.time.Instant;
import java.util.UUID;

public record ReviewDetailResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        ReviewReplyResponse reply
) {
    public static ReviewDetailResponse fromEntity(Review review, ReviewReply reply) {
        return new ReviewDetailResponse(
                review.getId(),
                review.getOrder().getId(),
                review.getCustomer().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getCreatedBy(),
                review.getUpdatedBy(),
                reply != null ? ReviewReplyResponse.fromEntity(reply) : null
        );
    }
}
