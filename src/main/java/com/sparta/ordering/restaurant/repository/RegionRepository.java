package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    List<Region> findByParentIsNullAndDeletedAtIsNull();

    List<Region> findByParent_IdAndDeletedAtIsNull(UUID parentId);

    boolean existsByParentIsNullAndNameAndDeletedAtIsNull(@NotBlank @Size(min = 1, max = 50, message = "지역 이름은 1자 이상 50자 이하여야 합니다.") String name);

    boolean existsByParent_IdAndNameAndDeletedAtIsNull(UUID id, @NotBlank @Size(min = 1, max = 50, message = "지역 이름은 1자 이상 50자 이하여야 합니다.") String name);

    boolean existsByParent_IdAndDeletedAtIsNull(UUID regionId);

    Optional<Region> findByIdAndDeletedAtIsNull(UUID regionId);
}
