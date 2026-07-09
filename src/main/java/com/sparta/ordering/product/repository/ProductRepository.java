package com.sparta.ordering.product.repository;

import com.sparta.ordering.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdAndRestaurant_User_IdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByIdAndRestaurant_User_IdAndDeletedAtIsNull(UUID id, UUID restaurantUserId);


    List<Product> findAllByIdInAndDeletedAtIsNull(List<UUID> productIds);

    @Query("""
                SELECT p FROM Product p
                JOIN FETCH p.restaurant
                WHERE p.id = :id AND p.deletedAt IS NULL
           """
    )
    Optional<Product> findByIdAndDeletedAtIsNullWithRestaurant(UUID id);

}
