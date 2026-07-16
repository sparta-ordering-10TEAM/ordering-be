package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    boolean existsByRegion_IdAndDeletedAtIsNull(UUID regionId);

    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

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

    @Query("""
                SELECT r
                FROM Restaurant r
                WHERE r.deletedAt IS NULL
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
                AND (:regionId IS NULL OR r.region.id = :regionId)
                AND (:status IS NULL OR r.status = :status)
            """)
    Page<Restaurant> findWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("regionId") UUID regionId,
            @Param("status") RestaurantStatus status,
            Pageable pageable);
}
