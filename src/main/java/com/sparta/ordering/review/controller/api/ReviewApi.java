package com.sparta.ordering.review.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Review", description = "리뷰 관리 API")
@RequestMapping("/api")
public interface ReviewApi {
    @Operation(
            summary = "리뷰 생성",
            description = "완료된 주문에 대해 평점과 리뷰 코멘트를 등록합니다. 생성된 리뷰 ID가 반환됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/orders/{orderId}/reviews")
    ResponseEntity<GeneralResponse<UUID>> postReview(
            @PathVariable UUID orderId,
            @RequestBody @Valid PostReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "가게 리뷰 목록 조회",
            description = "특정 식당의 전체 리뷰 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/restaurants/{restaurantId}/reviews")
    ResponseEntity<GeneralResponse<Page<ReviewResponse>>> searchRestaurantReviews(
            @PathVariable UUID restaurantId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "상품 리뷰 목록 조회",
            description = "특정 상품의 리뷰 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/products/{productId}/reviews")
    ResponseEntity<GeneralResponse<Page<ReviewResponse>>> searchProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "리뷰 수정",
            description = "자신이 작성한 리뷰의 평점과 내용을 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/reviews/{reviewId}")
    ResponseEntity<GeneralResponse<Void>> updateReview(
            @PathVariable UUID reviewId,
            @RequestBody @Valid UpdateReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "리뷰 삭제",
            description = "자신이 작성한 리뷰를 삭제(Soft Delete)하거나, 관리자가 강제 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/reviews/{reviewId}")
    ResponseEntity<GeneralResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserDetails user,
            Authentication authentication
    );

    @Operation(
            summary = "리뷰 단건 조회",
            description = "특정 리뷰의 상세 내용을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/reviews/{reviewId}")
    ResponseEntity<GeneralResponse<ReviewResponse>> getReview(
            @PathVariable UUID reviewId
    );
}
