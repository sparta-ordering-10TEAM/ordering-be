package com.sparta.ordering.cart.repository;

import com.sparta.ordering.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUser_IdAndDeletedAtIsNull(UUID userId);

}
