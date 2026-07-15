package com.sparta.ordering.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PGRequest(
        String paymentKey,
        UUID orderId,
        BigDecimal amount
) {
}
