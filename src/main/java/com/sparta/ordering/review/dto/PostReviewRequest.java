package com.sparta.ordering.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PostReviewRequest(
        @NotNull(message = "평점은 필수 입력값입니다.")
        @Min(value = 1, message = "평점은 최소 1점 이상만 가능합니다.")
        @Max(value = 5, message = "평점은 최대 1점 이하만 가능합니다.")
        int rating,

        String comment
) {
}
