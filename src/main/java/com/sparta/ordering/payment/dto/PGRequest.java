package com.sparta.ordering.payment.dto;

import java.util.UUID;

public record PGRequest(
        String paymentKey,
        UUID orderId,
        Long amount
) {
}
