package com.sparta.ordering.cart.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.user.entity.User;
import jakarta.persistence.Column;
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

import java.util.UUID;

@Entity
@Table(
        name = "p_carts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_user",
                        columnNames = {"user_id", "unique_version"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "unique_version", nullable = false, columnDefinition = "uuid")
    private UUID uniqueVersion;

    @Builder
    public Cart(User user, Restaurant restaurant) {
        this.user = user;
        this.restaurant = restaurant;
        this.uniqueVersion = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
        this.uniqueVersion = this.getId();
    }
}
