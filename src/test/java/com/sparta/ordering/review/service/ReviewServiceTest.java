package com.sparta.ordering.review.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderStatus;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.review.dto.ReviewResponse;
import com.sparta.ordering.review.entity.Review;
import com.sparta.ordering.review.repository.ReviewRepository;
import com.sparta.ordering.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Nested
    @DisplayName("리뷰 작성 (postReview)")
    class PostReview {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            int rating = 5;
            String comment = "맛있어요!";

            User customer = mock(User.class);

            Order order = mock(Order.class);
            when(order.getCustomer()).thenReturn(customer);
            when(order.getOrderStatus()).thenReturn(OrderStatus.COMPLETED);

            when(orderRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrder_IdAndCustomer_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(false);

            // when
            reviewService.postReview(rating, comment, orderId, userId);

            // then
            verify(reviewRepository, times(1)).save(any(Review.class));
        }

        @Test
        @DisplayName("실패 - 평점 범위를 벗어남 (1 미만)")
        void failInvalidRatingUnder() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            assertThatThrownBy(() -> reviewService.postReview(0, "댓글", orderId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.INVALID_REQUEST);

            verifyNoInteractions(orderRepository, reviewRepository);
        }

        @Test
        @DisplayName("실패 - 평점 범위를 벗어남 (5 초과)")
        void failInvalidRatingOver() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            assertThatThrownBy(() -> reviewService.postReview(6, "댓글", orderId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.INVALID_REQUEST);

            verifyNoInteractions(orderRepository, reviewRepository);
        }

        @Test
        @DisplayName("실패 - 주문을 찾을 수 없음")
        void failOrderNotFound() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(orderRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.postReview(4, "댓글", orderId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.ORDER_NOT_FOUND);

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("실패 - 주문이 완료 상태가 아님")
        void failOrderNotCompleted() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Order order = mock(Order.class);
            when(order.getOrderStatus()).thenReturn(OrderStatus.REQUESTED); // 완료 아님

            when(orderRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.of(order));

            assertThatThrownBy(() -> reviewService.postReview(4, "댓글", orderId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.ORDER_NOT_COMPLETED);

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("실패 - 이미 리뷰를 작성한 주문")
        void failAlreadyReviewed() {
            UUID orderId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Order order = mock(Order.class);
            when(order.getOrderStatus()).thenReturn(OrderStatus.COMPLETED);

            when(orderRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrder_IdAndCustomer_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(true);

            assertThatThrownBy(() -> reviewService.postReview(4, "댓글", orderId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.ALREADY_REVIEWED);

            verify(reviewRepository, never()).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("가게 리뷰 조회 (searchRestaurantReviews)")
    class SearchRestaurantReviews {

        @Test
        @DisplayName("성공")
        void success() {
            UUID restaurantId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)).thenReturn(true);

            User customer = mock(User.class);
            when(customer.getId()).thenReturn(UUID.randomUUID());

            Order order = mock(Order.class);
            when(order.getId()).thenReturn(UUID.randomUUID());

            Review review = Review.builder()
                    .customer(customer)
                    .order(order)
                    .rating(5)
                    .comment("맛나요")
                    .build();
            ReflectionTestUtils.setField(review, "id", UUID.randomUUID());

            Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);
            when(reviewRepository.findByOrder_Restaurant_IdAndDeletedAtIsNull(restaurantId, pageable))
                    .thenReturn(reviewPage);

            Page<ReviewResponse> result = reviewService.searchRestaurantReviews(restaurantId, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).comment()).isEqualTo("맛나요");
            assertThat(result.getContent().get(0).rating()).isEqualTo(5);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 가게")
        void failRestaurantNotFound() {
            UUID restaurantId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.searchRestaurantReviews(restaurantId, pageable))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("상품 리뷰 조회 (searchProductReviews)")
    class SearchProductReviews {

        @Test
        @DisplayName("성공")
        void success() {
            UUID productId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.existsByIdAndDeletedAtIsNull(productId)).thenReturn(true);

            User customer = mock(User.class);
            when(customer.getId()).thenReturn(UUID.randomUUID());

            Order order = mock(Order.class);
            when(order.getId()).thenReturn(UUID.randomUUID());

            Review review = Review.builder()
                    .customer(customer)
                    .order(order)
                    .rating(4)
                    .comment("상품 최고")
                    .build();
            ReflectionTestUtils.setField(review, "id", UUID.randomUUID());

            Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);
            when(reviewRepository.findByProductIdAndDeletedAtIsNull(productId, pageable))
                    .thenReturn(reviewPage);

            Page<ReviewResponse> result = reviewService.searchProductReviews(productId, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).comment()).isEqualTo("상품 최고");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void failProductNotFound() {
            UUID productId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.existsByIdAndDeletedAtIsNull(productId)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.searchProductReviews(productId, pageable))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("가게 평균 평점 조회 (getRestaurantAverageRating)")
    class GetRestaurantAverageRating {

        @Test
        @DisplayName("성공")
        void success() {
            UUID restaurantId = UUID.randomUUID();
            when(restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)).thenReturn(true);
            when(reviewRepository.calcRestaurantAverageRating(restaurantId)).thenReturn(4.5);

            double averageRating = reviewService.getRestaurantAverageRating(restaurantId);

            assertThat(averageRating).isEqualTo(4.5);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 가게")
        void failRestaurantNotFound() {
            UUID restaurantId = UUID.randomUUID();
            when(restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.getRestaurantAverageRating(restaurantId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리뷰 수정 (updateReview)")
    class UpdateReview {

        @Test
        @DisplayName("성공")
        void success() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Review review = spy(Review.builder().rating(3).comment("그냥 그래요").build());
            when(reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(reviewId, userId))
                    .thenReturn(Optional.of(review));

            reviewService.updateReview(5, "수정된 평점 최고!", reviewId, userId);

            verify(review, times(1)).updateReview(5, "수정된 평점 최고!");
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getComment()).isEqualTo("수정된 평점 최고!");
        }

        @Test
        @DisplayName("성공 - 평점만 수정 (코멘트는 null로 들어와 기존 값 보존)")
        void successOnlyRating() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Review review = spy(Review.builder().rating(3).comment("그냥 그래요").build());
            when(reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(reviewId, userId))
                    .thenReturn(Optional.of(review));

            reviewService.updateReview(5, null, reviewId, userId);

            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getComment()).isEqualTo("그냥 그래요");
        }

        @Test
        @DisplayName("성공 - 코멘트만 수정 (평점은 null로 들어와 기존 값 보존)")
        void successOnlyComment() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Review review = spy(Review.builder().rating(3).comment("그냥 그래요").build());
            when(reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(reviewId, userId))
                    .thenReturn(Optional.of(review));

            reviewService.updateReview(null, "수정된 평점 최고!", reviewId, userId);

            assertThat(review.getRating()).isEqualTo(3);
            assertThat(review.getComment()).isEqualTo("수정된 평점 최고!");
        }

        @Test
        @DisplayName("실패 - 리뷰를 찾을 수 없거나 다른 유저의 리뷰")
        void failReviewNotFound() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(reviewId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview(5, "수정", reviewId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리뷰 논리 삭제 (softDeleteReview)")
    class SoftDeleteReview {

        @Test
        @DisplayName("성공")
        void success() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Review review = spy(Review.builder().rating(3).comment("그냥 그래요").build());
            ReflectionTestUtils.setField(review, "id", reviewId);
            when(reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(reviewId, userId))
                    .thenReturn(Optional.of(review));

            reviewService.softDeleteReview(reviewId, userId);

            verify(review, times(1)).softDelete(userId);
            assertThat(review.getDeletedAt()).isNotNull();
            assertThat(review.getDeletedBy()).isEqualTo(userId);
            assertThat(review.getUniqueVersion()).isEqualTo(reviewId);
        }

        @Test
        @DisplayName("실패 - 리뷰를 찾을 수 없거나 다른 유저의 리뷰")
        void failReviewNotFound() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNull(reviewId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.softDeleteReview(reviewId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REVIEW_NOT_FOUND);
        }
    }
}
