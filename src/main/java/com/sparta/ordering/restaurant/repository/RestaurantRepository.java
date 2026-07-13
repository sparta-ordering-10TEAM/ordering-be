package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByIdAndDeletedAtIsNull(UUID id);

    boolean existsByRegion_IdAndDeletedAtIsNull(UUID regionId);

    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    Optional<Restaurant> findByIdAndDeletedAtIsNull(UUID id);

    // ponytail: regionId는 해당 지역만 정확 매칭. 하위 지역 포함 조회는 지역 계층 재귀 조회 도입 시 확장.
    @Query("""
            select r from Restaurant r
            where r.deletedAt is null
              and (:categoryId is null or r.category.id = :categoryId)
              and (:regionId is null or r.region.id = :regionId)
              and (:status is null or r.status = :status)
            """)
    Page<Restaurant> findWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("regionId") UUID regionId,
            @Param("status") RestaurantStatus status,
            Pageable pageable
    );

    Page<Restaurant> findByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);
}
