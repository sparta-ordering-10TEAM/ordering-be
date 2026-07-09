package com.sparta.ordering.product.repository;

import com.sparta.ordering.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(UUID id);


    List<Product> findAllByIdInAndDeletedAtIsNull(List<UUID> productIds);

    @Query("""
                SELECT p FROM Product p
                JOIN FETCH p.restaurant
                WHERE p.id = :id AND p.deletedAt IS NULL
           """
    )
    Optional<Product> findByIdAndDeletedAtIsNullWithRestaurant(UUID id);

    @Query("""
                SELECT p FROM Product p
                WHERE p.restaurant.id = :restaurantId
                AND p.restaurant.deletedAt IS NULL
                AND p.deletedAt IS NULL
                AND (:name IS NULL OR p.name LIKE CONCAT('%', CAST(:name AS string), '%') )
                AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                AND (:minPrice IS NULL OR p.price >= :minPrice)
           """
    )
    Page<Product> searchProducts(UUID restaurantId, String name, Long minPrice, Long maxPrice, Pageable pageable);
}
