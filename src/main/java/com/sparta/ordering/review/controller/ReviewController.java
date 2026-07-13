package com.sparta.ordering.review.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.global.security.SecurityUtil;
import com.sparta.ordering.review.controller.api.ReviewApi;
import com.sparta.ordering.review.dto.PostReviewRequest;
import com.sparta.ordering.review.dto.ReviewResponse;
import com.sparta.ordering.review.dto.UpdateReviewRequest;
import com.sparta.ordering.review.service.AdminReviewService;
import com.sparta.ordering.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController implements ReviewApi {
    private final ReviewService reviewService;
    private final AdminReviewService adminReviewService;

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/orders/{orderId}/reviews")
    public ResponseEntity<GeneralResponse<UUID>> postReview(
            @PathVariable UUID orderId,
            @RequestBody @Valid PostReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID reviewId = reviewService.postReview(request.rating(), request.comment(), orderId, user.getUserId());

        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, reviewId);
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
    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<GeneralResponse<Void>> updateReview(
            @PathVariable UUID reviewId,
            @RequestBody @Valid UpdateReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        reviewService.updateReview(request.rating(), request.comment(), reviewId, user.getUserId());

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<GeneralResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserDetails user,
            Authentication authentication
    ) {
        boolean isAdmin = SecurityUtil.getRole(authentication).isAdmin();
        log.info("UserId : {}, role : {}", user.getUserId(), SecurityUtil.getRole(authentication));

        if (isAdmin) {
            adminReviewService.softDeleteReview(reviewId, user.getUserId());
        } else {
            reviewService.softDeleteReview(reviewId, user.getUserId());
        }

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

    @Override
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<GeneralResponse<ReviewResponse>> getReview(@PathVariable UUID reviewId) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                reviewService.getReview(reviewId)
        );
    }
}