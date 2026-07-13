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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final ProductRepository productRepository;

    @Transactional
    public UUID postReview(int rating, String comment, UUID orderId, UUID userId) {
        if (rating < 1 || rating > 5) { // 메서드 재활용 시 rating 범위에 대한 안전 장치
            throw new ApiException(GeneralResponseCode.INVALID_REQUEST);
        }

        Order order = orderRepository.findByIdAndUser_IdAndDeletedAtIsNull(orderId, userId) // 주문 조회 및 소유권 검증
                .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(GeneralResponseCode.ORDER_NOT_COMPLETED); // 완료되지 않은 주문에 대한 리뷰 작성 검증
        }

        if (reviewRepository.existsByOrder_IdAndCustomer_IdAndDeletedAtIsNull(orderId, userId)) {
            throw new ApiException(GeneralResponseCode.ALREADY_REVIEWED); // 리뷰 중복체크
        }

        Review newReview = Review.builder()
                .customer(order.getUser())
                .order(order)
                .rating(rating)
                .comment(comment)
                .build();

        reviewRepository.save(newReview);
        restaurantRepository.updateAverageRating(order.getRestaurant().getId());
        return newReview.getId();
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> searchRestaurantReviews(UUID restaurantId, Pageable pageable) {
        if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND);
        }

        return reviewRepository.findByOrder_Restaurant_IdAndDeletedAtIsNull(restaurantId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> searchProductReviews(UUID productId, Pageable pageable) {
        if (!productRepository.existsByIdAndDeletedAtIsNull(productId)) {
            throw new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND);
        }

        return reviewRepository.findByProductIdAndDeletedAtIsNull(productId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public double getRestaurantAverageRating(UUID restaurantId) {
        if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND);
        }

        return reviewRepository.calcRestaurantAverageRating(restaurantId);
    }

    @Transactional
    public void updateReview(Integer rating, String comment, UUID reviewId, UUID userId) {
        Review review = reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNullWithOrder(reviewId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.REVIEW_NOT_FOUND));

        review.updateReview(rating, comment);
        restaurantRepository.updateAverageRating(review.getOrder().getRestaurant().getId());
    }

    @Transactional
    public void softDeleteReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findByIdAndCustomer_IdAndDeletedAtIsNullWithOrder(reviewId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.REVIEW_NOT_FOUND));

        review.softDelete(userId);
        restaurantRepository.updateAverageRating(review.getOrder().getRestaurant().getId());
    }
}
