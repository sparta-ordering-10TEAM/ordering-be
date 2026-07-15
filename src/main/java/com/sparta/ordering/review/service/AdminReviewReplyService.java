package com.sparta.ordering.review.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.review.entity.Review;
import com.sparta.ordering.review.entity.ReviewReply;
import com.sparta.ordering.review.repository.ReviewReplyRepository;
import com.sparta.ordering.review.repository.ReviewRepository;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public UUID replyReview(UUID reviewId, UUID userId, String replyText) {
        validateAuthorities(userId);

        if (reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, userId)) {
            throw new ApiException(GeneralResponseCode.ALREADY_REVIEWED); // 리뷰 답변 중복체크
        }

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.REVIEW_NOT_FOUND));

        ReviewReply reviewReply = ReviewReply.builder()
                .review(review)
                .replyText(replyText)
                .build();

        ReviewReply savedReviewReply = reviewReplyRepository.save(reviewReply);

        return savedReviewReply.getId();
    }

    @Transactional
    public void softDeleteReviewReply(UUID reviewReplyId, UUID userId) {
        validateAuthorities(userId);

        ReviewReply reviewReply = reviewReplyRepository.findByIdAndDeletedAtIsNull(reviewReplyId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.REVIEW_REPLY_NOT_FOUND));

        reviewReply.softDelete(userId);
    }

    private void validateAuthorities(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!user.getRole().isAdmin()) { // 사용자 Role 검증
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }
    }
}
