package com.sparta.ordering.review.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.review.controller.api.ReviewApi;
import com.sparta.ordering.review.dto.PostReviewRequest;
import com.sparta.ordering.review.dto.ReviewResponse;
import com.sparta.ordering.review.dto.UpdateReviewRequest;
import com.sparta.ordering.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController implements ReviewApi {
    private final ReviewService reviewService;

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/orders/{orderId}/reviews")
    public ResponseEntity<GeneralResponse<Void>> postReview(
            @PathVariable UUID orderId,
            @RequestBody @Valid PostReviewRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        reviewService.postReview(request.rating(), request.comment(), orderId, userId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

    @Override
    @GetMapping("/restaurants/{restaurantId}/reviews")
    public ResponseEntity<GeneralResponse<Page<ReviewResponse>>> searchRestaurantReviews(
            @PathVariable UUID restaurantId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                reviewService.searchRestaurantReviews(restaurantId, pageable)
        );
    }

    @Override
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<GeneralResponse<Page<ReviewResponse>>> searchProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                reviewService.searchProductReviews(productId, pageable)
        );
    }

    @Override
    @GetMapping("/restaurants/{restaurantId}/ratings")
    public ResponseEntity<GeneralResponse<Double>> getRestaurantAverageRating(
            @PathVariable UUID restaurantId
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                reviewService.getRestaurantAverageRating(restaurantId)
        );
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<GeneralResponse<Void>> updateReview(
            @PathVariable UUID reviewId,
            @RequestBody @Valid UpdateReviewRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        reviewService.updateReview(request.rating(), request.comment(), reviewId, userId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<GeneralResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID userId
    ) {
        reviewService.softDeleteReview(reviewId, userId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}