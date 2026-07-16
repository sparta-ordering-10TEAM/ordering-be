package com.sparta.ordering.review.repository;

import com.sparta.ordering.review.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, UUID> {
    boolean existsByReview_IdAndCreatedByAndDeletedAtIsNull(UUID reviewId, UUID userId);

    Optional<ReviewReply> findByIdAndDeletedAtIsNull(UUID reviewReplyId);

    Optional<ReviewReply> findByIdAndCreatedByAndDeletedAtIsNull(UUID reviewReplyId, UUID userId);

    Optional<ReviewReply> findByReview_IdAndDeletedAtIsNull(UUID reviewId);
}
