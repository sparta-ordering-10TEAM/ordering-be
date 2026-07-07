package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Optional<Restaurant> findByIdAndDeletedAtIsNull(UUID id);
}
