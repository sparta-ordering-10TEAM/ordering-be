package com.sparta.ordering.cart.repository;

import com.sparta.ordering.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCart_IdAndDeletedAtIsNull(UUID cartId);

    Optional<CartItem> findByCart_IdAndProduct_IdAndDeletedAtIsNull(UUID cartId, UUID productId);
}
