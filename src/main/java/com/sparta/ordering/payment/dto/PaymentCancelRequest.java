package com.sparta.ordering.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCancelRequest(
        @NotBlank(message = "취소 사유는 필수입니다.")
        String reason
) {
}
