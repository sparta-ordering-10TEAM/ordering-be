package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    List<Region> findByParentIsNullAndDeletedAtIsNull();

    List<Region> findByParent_IdAndDeletedAtIsNull(UUID parentId);

    boolean existsByParentIsNullAndNameAndDeletedAtIsNull(String name);

    boolean existsByParent_IdAndNameAndDeletedAtIsNull(UUID id, String name);

    boolean existsByParent_IdAndDeletedAtIsNull(UUID regionId);

    Optional<Region> findByIdAndDeletedAtIsNull(UUID regionId);
}
