package com.sparta.ordering.cart.entity;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseUpdatableEntity {

    private static final int MAX_QUANTITY = 99;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Builder
    public CartItem(Cart cart, Product product, int quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    public void increaseQuantity(int amount) {
        if (this.quantity + amount > MAX_QUANTITY) {
            throw new ApiException(GeneralResponseCode.CART_ITEM_QUANTITY_EXCEEDED);
        }
        this.quantity += amount;
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
    }
}
