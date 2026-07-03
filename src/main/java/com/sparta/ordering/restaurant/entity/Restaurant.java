package com.sparta.ordering.restaurant.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "p_restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant extends BaseUpdatableEntity {

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String address;

    @Column(name = "address_detail", nullable = false)
    private String addressDetail;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(name = "min_order_amount", nullable = false)
    private Integer minOrderAmount;

    @Column(name = "delivery_fee", nullable = false)
    private Integer deliveryFee;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "delivery_radius_km", nullable = false, precision = 4, scale = 1)
    private BigDecimal deliveryRadiusKm;

    @Builder
    public Restaurant(
            UUID userId,
            RestaurantCategory category,
            String name,
            String phone,
            String description,
            String address,
            String addressDetail,
            String zipCode,
            Integer minOrderAmount,
            Integer deliveryFee,
            RestaurantStatus status,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal deliveryRadiusKm
    ) {
        this.userId = userId;
        this.category = category;
        this.name = name;
        this.phone = phone;
        this.description = description;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipCode = zipCode;
        this.minOrderAmount = minOrderAmount;
        this.deliveryFee = deliveryFee;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deliveryRadiusKm = deliveryRadiusKm;
    }
}
