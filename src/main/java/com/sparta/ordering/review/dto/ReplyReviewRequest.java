package com.sparta.ordering.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplyReviewRequest(
        @NotBlank(message = "리뷰 답변은 필수 입력값입니다.")
        String replyText
) {
}
