package com.sparta.ordering.ai.repository;

import com.sparta.ordering.ai.entity.AiProductDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiProductDescriptionRepository extends JpaRepository<AiProductDescription, UUID> {
}
