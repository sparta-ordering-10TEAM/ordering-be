package com.sparta.ordering.cart.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_carts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_user",
                        columnNames = {"user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseUpdatableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Builder
    public Cart(User user, Restaurant restaurant) {
        this.user = user;
        this.restaurant = restaurant;
    }

    public void changeRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }
}
