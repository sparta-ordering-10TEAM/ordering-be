package com.sparta.ordering.review.controller.api;

import com.sparta.ordering.global.dto.GeneralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "관리자 - 리뷰 관리", description = "관리자 리뷰 관리 API")
@RequestMapping("/api/admin")
public interface AdminReviewApi {
    @Operation(
            summary = "리뷰 강제 삭제",
            description = "[MANAGER, MASTER 기능] 특정 리뷰를 삭제(Soft Delete)합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/reviews/{reviewId}")
    ResponseEntity<GeneralResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID userId
    );
}
