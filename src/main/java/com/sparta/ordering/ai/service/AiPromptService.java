package com.sparta.ordering.ai.service;

import com.sparta.ordering.ai.entity.AiPromptLog;
import com.sparta.ordering.ai.entity.PromptType;
import com.sparta.ordering.ai.repository.AiPromptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiPromptService {
    private final AiPromptLogRepository aiPromptLogRepository;

    @Transactional
    public void savePromptLog(String prompt, String responseText, PromptType promptType) {
        AiPromptLog log = AiPromptLog.builder()
                .prompt(prompt)
                .responseText(responseText)
                .promptType(promptType)
                .build();
        aiPromptLogRepository.save(log);
    }
}
