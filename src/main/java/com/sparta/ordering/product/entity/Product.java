package com.sparta.ordering.product.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseUpdatableEntity {


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "restaurant_id", nullable = false)
//    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long price;

    @Builder
    public Product(String name, String description, Long price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
