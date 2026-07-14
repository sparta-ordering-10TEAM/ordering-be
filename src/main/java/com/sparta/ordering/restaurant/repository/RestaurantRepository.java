package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Optional<Restaurant> findByIdAndDeletedAtIsNull(UUID id);

    Page<Restaurant> findByDeletedAtIsNull(Pageable pageable);

    Page<Restaurant> findByCategoryAndDeletedAtIsNull(RestaurantCategory category, Pageable pageable);

    Page<Restaurant> findByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true) // flushAutomatically는 현재 쓰기 지연된 쿼리들 모두 flush
    @Query("""
                UPDATE Restaurant r
                SET r.averageRating = (
                    SELECT COALESCE(AVG(rev.rating), 0.0)
                    FROM Review rev
                    WHERE rev.order.restaurant.id = :restaurantId
                    AND rev.deletedAt IS NULL
                ),
                r.reviewCount = (
                    SELECT COUNT(rev)
                    FROM Review rev
                    WHERE rev.order.restaurant.id = :restaurantId
                    AND rev.deletedAt IS NULL
                )
                WHERE r.id = :restaurantId
            """)
    int updateAverageRating(UUID restaurantId);
}
