package com.sparta.ordering.payment.dto;

import java.time.Instant;

public record PGCancelResponse (
        Instant canceledAt, String reason
){
}
