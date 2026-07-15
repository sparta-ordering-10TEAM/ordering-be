package com.sparta.ordering.payment.dto;

public record PGCancelRequest(
        String paymentKey,
        String reason
) {
}
