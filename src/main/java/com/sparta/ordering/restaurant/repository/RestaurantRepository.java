package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Optional<Restaurant> findByIdAndDeletedAtIsNull(UUID id);

    Page<Restaurant> findByDeletedAtIsNull(Pageable pageable);

    Page<Restaurant> findByCategoryAndDeletedAtIsNull(RestaurantCategory category, Pageable pageable);

    Page<Restaurant> findByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);
}
