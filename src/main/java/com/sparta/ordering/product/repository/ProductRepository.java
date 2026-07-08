package com.sparta.ordering.product.repository;

import com.sparta.ordering.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdAndRestaurant_User_IdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByIdAndRestaurant_User_IdAndDeletedAtIsNull(UUID id, UUID restaurantUserId);
}
