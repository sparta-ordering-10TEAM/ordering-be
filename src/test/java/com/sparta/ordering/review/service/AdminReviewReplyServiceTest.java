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
class AdminReviewReplyServiceTest {

    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private AdminReviewReplyService adminReviewReplyService;

    @Nested
    @DisplayName("어드민 리뷰 답글 작성 (replyReview)")
    class ReplyReview {

        @Test
        @DisplayName("성공 - MANAGER 권한으로 리뷰 답글 작성")
        void successManager() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            String replyText = "어드민이 남긴 답글입니다.";

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MANAGER);

            Review review = mock(Review.class);
            ReviewReply reviewReply = mock(ReviewReply.class);
            when(reviewReply.getId()).thenReturn(UUID.randomUUID());

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, adminId)).thenReturn(false);
            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
            when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(reviewReply);

            // when
            UUID result = adminReviewReplyService.replyReview(reviewId, adminId, replyText);

            // then
            assertThat(result).isNotNull();
            verify(reviewReplyRepository, times(1)).save(any(ReviewReply.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void failUserNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReviewReplyService.replyReview(reviewId, adminId, "답글"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 어드민 권한이 아닌 사용자가 시도 시 403 Forbidden")
        void failForbidden() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getRole()).thenReturn(Role.OWNER); // OWNER는 일반 답글 서비스 사용해야 함

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> adminReviewReplyService.replyReview(reviewId, adminId, "답글"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);
        }

        @Test
        @DisplayName("실패 - 이미 답변을 작성한 경우")
        void failAlreadyReplied() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MANAGER);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, adminId)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> adminReviewReplyService.replyReview(reviewId, adminId, "답글"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.ALREADY_REVIEWED);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 리뷰")
        void failReviewNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MANAGER);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewReplyRepository.existsByReview_IdAndCreatedByAndDeletedAtIsNull(reviewId, adminId)).thenReturn(false);
            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReviewReplyService.replyReview(reviewId, adminId, "답글"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("어드민 리뷰 답글 강제 삭제 (softDeleteReviewReply)")
    class SoftDeleteReviewReply {

        @Test
        @DisplayName("성공 - MASTER 권한으로 타인의 답글 강제 삭제")
        void successMaster() {
            // given
            UUID replyId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MASTER);

            ReviewReply reviewReply = mock(ReviewReply.class);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewReplyRepository.findByIdAndDeletedAtIsNull(replyId)).thenReturn(Optional.of(reviewReply));

            // when
            adminReviewReplyService.softDeleteReviewReply(replyId, adminId);

            // then
            verify(reviewReply, times(1)).softDelete(adminId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 답글 강제 삭제 시도 시 예외")
        void failReplyNotFound() {
            // given
            UUID replyId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MANAGER);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewReplyRepository.findByIdAndDeletedAtIsNull(replyId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReviewReplyService.softDeleteReviewReply(replyId, adminId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_REPLY_NOT_FOUND);
        }
    }
}
