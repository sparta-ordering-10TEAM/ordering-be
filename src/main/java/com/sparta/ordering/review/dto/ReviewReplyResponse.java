package com.sparta.ordering.review.dto;

import com.sparta.ordering.review.entity.ReviewReply;

import java.time.Instant;
import java.util.UUID;

public record ReviewReplyResponse(
        UUID id,
        String replyText,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
    public static ReviewReplyResponse fromEntity(ReviewReply reply) {
        return new ReviewReplyResponse(
                reply.getId(),
                reply.getReplyText(),
                reply.getCreatedAt(),
                reply.getUpdatedAt(),
                reply.getCreatedBy()
        );
    }
}
