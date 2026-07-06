package com.sparta.ordering.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequestDto {

    @NotNull
    private UUID restaurantId;

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotNull
    @Positive
    private Long price;
}
