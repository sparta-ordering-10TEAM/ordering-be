package com.sparta.ordering.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RestaurantCreateRequest(
        @NotBlank
        @Size(min = 1, max = 20, message = "카테고리는 1자 이상 20자 이하여야 합니다.")
        String category,

        @NotNull(message = "지역은 필수 입력 값입니다.")
        UUID regionId,

        @NotBlank
        @Size(min = 1, max = 100, message = "식당 이름은 1자 이상 100자 이하여야 합니다.")
        String name,

        @NotBlank
        @Size(min = 1, max = 20, message = "전화번호는 1자 이상이어야 합니다.")
        @Pattern(
                regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phone,

        @Size(max = 1000)
        String description,

        @NotBlank
        @Size(max = 255)
        String address,

        @NotBlank
        @Size(max = 255)
        String addressDetail,

        @NotBlank
        @Size(min = 1, max = 10, message = "우편번호는 1자 이상이어야 합니다.")
        String zipCode,

        @NotNull
        @Min(value = 0, message = "최소 주문 금액은 0원 이상이어야 합니다.")
        Integer minOrderAmount,

        @NotNull
        @Min(value = 0, message = "배달료는 0원 이상이어야 합니다.")
        Integer deliveryFee,

        @NotNull
        @DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90.0 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7)
        BigDecimal latitude,

        @NotNull
        @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180.0 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7)
        BigDecimal longitude,

        @NotNull
        @DecimalMin(value = "0.1", message = "배달 반경은 0.1km 이상이어야 합니다.")
        @Digits(integer = 3, fraction = 1)
        BigDecimal deliveryRadiusKm
) {
}
