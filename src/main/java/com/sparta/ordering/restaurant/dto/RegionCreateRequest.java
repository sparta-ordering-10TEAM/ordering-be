package com.sparta.ordering.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegionCreateRequest(
        @NotBlank
        @Size(min = 1, max = 50, message = "지역 이름은 1자 이상 50자 이하여야 합니다.")
        String name,

        UUID parentId
) {
}
