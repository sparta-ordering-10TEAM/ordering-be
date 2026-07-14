package com.sparta.ordering.payment.dto;

import com.sparta.ordering.payment.entity.Payment;
import com.sparta.ordering.payment.entity.PaymentMethod;
import com.sparta.ordering.payment.entity.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        PaymentMethod paymentMethod,
        Long amount,
        PaymentStatus status,
        String cancelReason,
        Instant canceledAt,
        Instant approvedAt,
        String cardCompany
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCancelReason(),
                payment.getCanceledAt(),
                payment.getApprovedAt(),
                payment.getCardCompany()
        );
    }
}
