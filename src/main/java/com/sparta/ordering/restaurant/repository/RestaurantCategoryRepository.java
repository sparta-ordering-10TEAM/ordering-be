package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategory, UUID> {
    Optional<RestaurantCategory> findByCodeAndDeletedAtIsNull(String category);

    Optional<RestaurantCategory> findByIdAndDeletedAtIsNull(UUID id);

    List<RestaurantCategory> findByDeletedAtIsNull();

    boolean existsByCodeAndDeletedAtIsNull(String code);
}
