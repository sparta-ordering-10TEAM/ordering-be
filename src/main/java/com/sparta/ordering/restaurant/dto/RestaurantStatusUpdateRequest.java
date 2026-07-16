package com.sparta.ordering.restaurant.dto;

import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import jakarta.validation.constraints.NotNull;

public record RestaurantStatusUpdateRequest(
        @NotNull(message = "영업 상태는 필수 입력값입니다.")
        RestaurantStatus status
) {
}
