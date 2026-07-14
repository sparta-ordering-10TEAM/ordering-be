package com.sparta.ordering.payment.dto;

import java.time.Instant;

public record PGResponse(
        Instant approvedAt,
        String cardCompany
){

}
