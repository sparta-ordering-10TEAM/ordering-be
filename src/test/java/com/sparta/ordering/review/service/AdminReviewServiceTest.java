package com.sparta.ordering.review.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.review.entity.Review;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminReviewService adminReviewService;

    @Nested
    @DisplayName("관리자 리뷰 강제 삭제 (softDeleteReview)")
    class SoftDeleteReview {

        @Test
        @DisplayName("성공 - MANAGER 권한으로 다른 사용자의 리뷰 삭제")
        void successManager() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MANAGER);

            Review review = spy(Review.builder().rating(4).comment("무난합니다").build());
            ReflectionTestUtils.setField(review, "id", reviewId);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

            // when
            adminReviewService.softDeleteReview(reviewId, adminId);

            // then
            verify(review, times(1)).softDelete(adminId);
            assertThat(review.getDeletedAt()).isNotNull();
            assertThat(review.getDeletedBy()).isEqualTo(adminId);
            assertThat(review.getUniqueVersion()).isEqualTo(reviewId);
        }

        @Test
        @DisplayName("성공 - MASTER 권한으로 다른 사용자의 리뷰 삭제")
        void successMaster() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MASTER);

            Review review = spy(Review.builder().rating(5).comment("최고의 맛!").build());
            ReflectionTestUtils.setField(review, "id", reviewId);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

            // when
            adminReviewService.softDeleteReview(reviewId, adminId);

            // then
            verify(review, times(1)).softDelete(adminId);
            assertThat(review.getDeletedAt()).isNotNull();
            assertThat(review.getDeletedBy()).isEqualTo(adminId);
            assertThat(review.getUniqueVersion()).isEqualTo(reviewId);
        }

        @Test
        @DisplayName("실패 - 삭제를 요청한 관리자 유저가 존재하지 않음")
        void failUserNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReviewService.softDeleteReview(reviewId, adminId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);

            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("실패 - 삭제를 요청한 유저가 관리자 권한(MANAGER, MASTER)이 아님")
        void failForbidden() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            User customer = mock(User.class);
            when(customer.getRole()).thenReturn(Role.CUSTOMER);

            when(userRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));

            // when & then
            assertThatThrownBy(() -> adminReviewService.softDeleteReview(reviewId, customerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("실패 - 존재하지 않거나 이미 삭제된 리뷰")
        void failReviewNotFound() {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(Role.MASTER);

            when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(admin));
            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminReviewService.softDeleteReview(reviewId, adminId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_NOT_FOUND);
        }
    }
}
