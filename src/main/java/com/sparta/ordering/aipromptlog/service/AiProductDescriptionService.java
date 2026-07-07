package com.sparta.ordering.aipromptlog.service;

import com.sparta.ordering.aipromptlog.dto.AiProductDescriptionResponse;
import jakarta.validation.constraints.NotBlank;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AiProductDescriptionService {
    public Page<AiProductDescriptionResponse> search(UUID productId, UUID userId, Pageable pageable) {
        return null;
    }

    public void generate(UUID productId, UUID userId, String prompt) {
    }

    public void update(UUID aiDescriptionId, UUID userId, @NotBlank(message = "상품 설명은 필수 입력값입니다.") String description) {
    }

    public void delete(UUID aiDescriptionId, UUID userId) {
    }
}
