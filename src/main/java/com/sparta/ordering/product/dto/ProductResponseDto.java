package com.sparta.ordering.product.dto;

import com.sparta.ordering.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductResponseDto {

    private UUID id;
    private UUID restaurantId;
    private String name;
    private String description;
    private Long price;

    public static ProductResponseDto from(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .restaurantId(product.getRestaurant().getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }
}
