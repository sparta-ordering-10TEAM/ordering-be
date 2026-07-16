package com.sparta.ordering.restaurant.entity;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "p_restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private RestaurantCategory category;

    // 기존 dev 데이터 호환을 위해 nullable. 적용 완료 후 NOT NULL 전환 예정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "delivery_radius_km", nullable = false, precision = 4, scale = 1)
    private BigDecimal deliveryRadiusKm;

    @Column(name = "average_rating", nullable = false)
    private Double averageRating = 0.0;

    @Column(name = "review_count", nullable = false)
    private Long reviewCount = 0L;

    @Builder
    public Restaurant(
            User user,
            RestaurantCategory category,
            Region region,
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
        this.user = user;
        this.category = category;
        this.region = region;
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
        this.averageRating = 0.0;
        this.reviewCount = 0L;
    }

    public void update(
            RestaurantCategory category,
            Region region,
            String name,
            String phone,
            String description,
            String address,
            String addressDetail,
            String zipCode,
            Integer minOrderAmount,
            Integer deliveryFee,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal deliveryRadiusKm
    ) {
        if (category != null) {
            this.category = category;
        }
        if (region != null) {
            this.region = region;
        }
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (description != null) {
            this.description = description;
        }
        if (address != null) {
            this.address = address;
        }
        if (addressDetail != null) {
            this.addressDetail = addressDetail;
        }
        if (zipCode != null) {
            this.zipCode = zipCode;
        }
        if (minOrderAmount != null) {
            this.minOrderAmount = minOrderAmount;
        }
        if (deliveryFee != null) {
            this.deliveryFee = deliveryFee;
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
        if (deliveryRadiusKm != null) {
            this.deliveryRadiusKm = deliveryRadiusKm;
        }
    }

    public void changeStatus(RestaurantStatus status) {
        if (status == null) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_STATUS_INVALID);
        }
        this.status = status;
    }

    public boolean isOwnedBy(User user) {
        return this.user != null
                && user != null
                && this.user.getId() != null
                && this.user.getId().equals(user.getId());
    }
}
