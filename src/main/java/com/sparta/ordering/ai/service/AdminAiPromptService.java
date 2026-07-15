package com.sparta.ordering.ai.service;

import com.sparta.ordering.ai.dto.AiPromptLogResponse;
import com.sparta.ordering.ai.entity.AiPromptLog;
import com.sparta.ordering.ai.repository.AiPromptLogRepository;
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
public class AdminAiPromptService {
    private final AiPromptLogRepository aiPromptLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void delete(UUID logId, UUID adminId) {
        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!admin.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        AiPromptLog log = aiPromptLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.AI_PROMPT_LOG_NOT_FOUND));

        log.softDelete(adminId);
    }

    @Transactional(readOnly = true)
    public Page<AiPromptLogResponse> search(UUID adminId, Pageable pageable) {
        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!admin.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        return aiPromptLogRepository.findByDeletedAtIsNull(pageable)
                .map(AiPromptLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AiPromptLogResponse getPromptLog(UUID logId, UUID adminId) {
        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!admin.getRole().isAdmin()) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }

        AiPromptLog log = aiPromptLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.AI_PROMPT_LOG_NOT_FOUND));

        return AiPromptLogResponse.fromEntity(log);
    }
}
