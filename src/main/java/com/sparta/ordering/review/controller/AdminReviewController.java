package com.sparta.ordering.review.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.review.controller.api.AdminReviewApi;
import com.sparta.ordering.review.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminReviewController implements AdminReviewApi {
    private final AdminReviewService adminReviewService;

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<GeneralResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID userId
    ) {
        adminReviewService.softDeleteReview(reviewId, userId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}
