package com.sparta.ordering.restaurant.entity;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.global.exception.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_restaurant_category")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RestaurantCategory extends BaseUpdatableEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Builder
    public RestaurantCategory(String code) {
        this.code = code;
    }

    public void updateCode(String code) {
        if (code == null) {
            throw new ApiException(GeneralResponseCode.RESTAURANT_CATEGORY_CODE_INVALID);
        }
        this.code = code;
    }

}
