package com.sparta.ordering.cart.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "p_cart_items")
public class CartItem extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;
}
