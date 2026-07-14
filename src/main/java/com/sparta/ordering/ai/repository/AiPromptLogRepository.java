package com.sparta.ordering.ai.repository;

import com.sparta.ordering.ai.entity.AiPromptLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiPromptLogRepository extends JpaRepository<AiPromptLog, UUID> {
    Page<AiPromptLog> findByDeletedAtIsNull(Pageable pageable);

    Optional<AiPromptLog> findByIdAndDeletedAtIsNull(UUID id);
}
