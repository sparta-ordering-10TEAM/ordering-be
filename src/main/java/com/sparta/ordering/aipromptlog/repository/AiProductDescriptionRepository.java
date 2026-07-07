package com.sparta.ordering.aipromptlog.repository;

import com.sparta.ordering.aipromptlog.entity.AiProductDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiProductDescriptionRepository extends JpaRepository<AiProductDescription, UUID> {
}
