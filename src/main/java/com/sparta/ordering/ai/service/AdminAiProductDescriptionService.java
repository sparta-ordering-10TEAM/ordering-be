package com.sparta.ordering.ai.service;

import com.sparta.ordering.ai.dto.AiProductDescriptionResponse;
import com.sparta.ordering.ai.entity.AiProductDescription;
import com.sparta.ordering.ai.repository.AiProductDescriptionRepository;
import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAiProductDescriptionService {
    private final AiProductDescriptionRepository aiProductDescriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void delete(UUID aiDescriptionId, UUID adminId) {
        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!admin.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        AiProductDescription aiProductDescription = aiProductDescriptionRepository.findByIdAndDeletedAtIsNull(aiDescriptionId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.AI_PRODUCT_DESCRIPTION_NOT_FOUND));

        aiProductDescription.softDelete(adminId);
    }

    @Transactional(readOnly = true)
    public Page<AiProductDescriptionResponse> search(UUID productId, UUID adminId, Pageable pageable) {
        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!admin.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        return aiProductDescriptionRepository.findByProductIdAndDeletedAtIsNull(productId, pageable)
                .map(AiProductDescriptionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AiProductDescriptionResponse getAiProductDescription(UUID aiDescriptionId, UUID adminId) {
        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!admin.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        AiProductDescription aiProductDescription = aiProductDescriptionRepository.findByIdAndDeletedAtIsNull(aiDescriptionId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.AI_PRODUCT_DESCRIPTION_NOT_FOUND));

        return AiProductDescriptionResponse.fromEntity(aiProductDescription);
    }
}
