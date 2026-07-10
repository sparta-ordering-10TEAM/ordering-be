package com.sparta.ordering.cart.repository;

import com.sparta.ordering.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Query("""
                SELECT ci FROM CartItem ci
                JOIN FETCH ci.product
                WHERE ci.cart.id = :cartId AND ci.deletedAt IS NULL
           """
    )
    List<CartItem> findByCart_IdAndDeletedAtIsNullWithProduct(UUID cartId);

    Optional<CartItem> findByCart_IdAndProduct_IdAndDeletedAtIsNull(UUID cartId, UUID productId);

    @Query("""
                SELECT ci FROM CartItem ci
                JOIN FETCH ci.cart c
                WHERE ci.id = :cartItemId AND c.user.id = :userId AND ci.deletedAt IS NULL
           """
    )
    Optional<CartItem> findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(UUID cartItemId, UUID userId);
}
