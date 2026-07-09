package com.sparta.ordering.product.dto;

import jakarta.validation.constraints.Positive;

public record ProductSearchRequest (
        String name,
        @Positive(message = "가격은 0보다 커야 합니다.")
        Long minPrice,
        @Positive(message = "가격은 0보다 커야 합니다.")
        Long maxPrice
){
}
