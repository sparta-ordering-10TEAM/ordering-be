package com.sparta.ordering.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateReviewRequest(
        @Min(value = 1, message = "평점은 최소 1점 이상만 가능합니다.")
        @Max(value = 5, message = "평점은 최대 5점 이하만 가능합니다.")
        Integer rating,

        String comment
) {
}
