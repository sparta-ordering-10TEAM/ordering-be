package com.sparta.ordering.ai.repository;

import com.sparta.ordering.ai.entity.AiProductDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiProductDescriptionRepository extends JpaRepository<AiProductDescription, UUID> {
    Page<AiProductDescription> findByProductIdAndDeletedAtIsNull(UUID productId, Pageable pageable);

    Optional<AiProductDescription> findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(UUID id, UUID userId);
}
