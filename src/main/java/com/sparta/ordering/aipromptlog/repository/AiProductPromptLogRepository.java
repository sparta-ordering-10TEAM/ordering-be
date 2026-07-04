package com.sparta.ordering.aipromptlog.repository;

import com.sparta.ordering.aipromptlog.entity.AiProductPromptLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiProductPromptLogRepository extends JpaRepository<AiProductPromptLog, UUID> {
}
