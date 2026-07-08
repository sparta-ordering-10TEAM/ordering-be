package com.sparta.ordering.product.dto;

public record ProductSearchRequest (
        String name,
        Long minPrice,
        Long maxPrice
){
}
