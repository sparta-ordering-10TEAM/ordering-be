package com.sparta.ordering.review.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.review.entity.Review;
import com.sparta.ordering.review.entity.ReviewReply;
import com.sparta.ordering.review.repository.ReviewReplyRepository;
import com.sparta.ordering.review.repository.ReviewRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewReplyServiceTest {

    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewReplyService reviewReplyService;

    @Nested
    @DisplayName("리뷰 답글 작성 (replyReview)")
    class ReplyReview {

        @Test
        @DisplayName("성공 - OWNER 권한으로 본인 상점의 리뷰에 답글 작성")
        void success() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String replyText = "감사합니다!";

            User user = mock(User.class);
            when(user.getRole()).thenReturn(Role.OWNER);

            Review review = mock(Review.class);

            ReviewReply reviewReply = mock(ReviewReply.class);
            when(reviewReply.getId()).thenReturn(UUID.randomUUID());

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, userId)).thenReturn(false);
            when(reviewRepository.findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(reviewId, userId)).thenReturn(Optional.of(review));
            when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(reviewReply);

            // when
            UUID result = reviewReplyService.replyReview(reviewId, userId, replyText);

            // then
            assertThat(result).isNotNull();
            verify(reviewReplyRepository, times(1)).save(any(ReviewReply.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void failUserNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewReplyService.replyReview(reviewId, userId, "감사합니다!"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining(GeneralResponseCode.USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - OWNER 권한이 아닌 사용자가 답글 작성 시도 시 403 Forbidden")
        void failForbidden() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getRole()).thenReturn(Role.CUSTOMER);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> reviewReplyService.replyReview(reviewId, userId, "감사합니다!"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);
        }

        @Test
        @DisplayName("실패 - 이미 답변을 작성한 경우 중복 작성 예외")
        void failAlreadyReplied() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getRole()).thenReturn(Role.OWNER);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, userId)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewReplyService.replyReview(reviewId, userId, "감사합니다!"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.ALREADY_REVIEWED);
        }

        @Test
        @DisplayName("실패 - 본인 가게의 리뷰가 아닌 경우 조회 불가")
        void failReviewNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getRole()).thenReturn(Role.OWNER);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, userId)).thenReturn(false);
            when(reviewRepository.findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(reviewId, userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewReplyService.replyReview(reviewId, userId, "감사합니다!"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리뷰 답글 수정 (updateReviewReply)")
    class UpdateReviewReply {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID replyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String newText = "새로운 내용";

            ReviewReply reviewReply = mock(ReviewReply.class);
            when(reviewReplyRepository.findByIdAndCreatedByAndDeletedAtIsNull(replyId, userId)).thenReturn(Optional.of(reviewReply));

            // when
            reviewReplyService.updateReviewReply(replyId, userId, newText);

            // then
            verify(reviewReply, times(1)).update(newText);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 답글")
        void failNotFound() {
            // given
            UUID replyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(reviewReplyRepository.findByIdAndCreatedByAndDeletedAtIsNull(replyId, userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewReplyService.updateReviewReply(replyId, userId, "새로운 내용"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_REPLY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리뷰 답글 삭제 (softDeleteReviewReply)")
    class SoftDeleteReviewReply {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID replyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            ReviewReply reviewReply = mock(ReviewReply.class);
            when(reviewReplyRepository.findByIdAndCreatedByAndDeletedAtIsNull(replyId, userId)).thenReturn(Optional.of(reviewReply));

            // when
            reviewReplyService.softDeleteReviewReply(replyId, userId);

            // then
            verify(reviewReply, times(1)).softDelete(userId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 답글")
        void failNotFound() {
            // given
            UUID replyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(reviewReplyRepository.findByIdAndCreatedByAndDeletedAtIsNull(replyId, userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewReplyService.softDeleteReviewReply(replyId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_REPLY_NOT_FOUND);
        }
    }
}
