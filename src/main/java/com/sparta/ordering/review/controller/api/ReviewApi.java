package com.sparta.ordering.review.controller.api;

import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.review.dto.PostReviewRequest;
import com.sparta.ordering.review.dto.ReviewResponse;
import com.sparta.ordering.review.dto.UpdateReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Review", description = "리뷰 관리 API")
public interface ReviewApi {
    @Operation(
            summary = "리뷰 생성",
            description = "완료된 주문에 대해 평점과 리뷰 코멘트를 등록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Void>> postReview(
            UUID orderId,
            @Valid PostReviewRequest request,
            UUID userId
    );

    @Operation(
            summary = "가게 리뷰 목록 조회",
            description = "특정 식당의 전체 리뷰 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Page<ReviewResponse>>> searchRestaurantReviews(
            UUID restaurantId,
            Pageable pageable
    );

    @Operation(
            summary = "상품 리뷰 목록 조회",
            description = "특정 상품의 리뷰 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Page<ReviewResponse>>> searchProductReviews(
            UUID productId,
            Pageable pageable
    );

    @Operation(
            summary = "가게 평균 평점 조회",
            description = "특정 식당의 누적 리뷰 평균 평점을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Double>> getRestaurantAverageRating(
            UUID restaurantId
    );

    @Operation(
            summary = "리뷰 수정",
            description = "자신이 작성한 리뷰의 평점과 내용을 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Void>> updateReview(
            UUID reviewId,
            @Valid UpdateReviewRequest request,
            UUID userId
    );

    @Operation(
            summary = "리뷰 삭제",
            description = "자신이 작성한 리뷰를 삭제(Soft Delete)합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Void>> deleteReview(
            UUID reviewId,
            UUID userId
    );
}
