package com.sparta.ordering.ai.controller;

import com.sparta.ordering.ai.controller.api.AiPromptApi;
import com.sparta.ordering.ai.dto.AiPromptLogResponse;
import com.sparta.ordering.ai.dto.GenerateProductDescriptionRequest;
import com.sparta.ordering.ai.facade.AiPromptFacade;
import com.sparta.ordering.ai.service.AdminAiPromptService;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AiPromptController implements AiPromptApi {
    private final AiPromptFacade aiPromptFacade;
    private final AdminAiPromptService adminAiPromptService;

    @Override
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/products/generate-description")
    public ResponseEntity<GeneralResponse<String>> generateProductDescription(
            @RequestBody @Valid GenerateProductDescriptionRequest request
    ) {
        String description = aiPromptFacade.generateProductDescription(request.prompt());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, description);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping("/admin/ai-prompts")
    public ResponseEntity<GeneralResponse<Page<AiPromptLogResponse>>> searchAiPromptLogs(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AiPromptLogResponse> response = adminAiPromptService.search(user.getUserId(), pageable);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping("/admin/ai-prompts/{logId}")
    public ResponseEntity<GeneralResponse<AiPromptLogResponse>> getAiPromptLog(
            @PathVariable UUID logId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        AiPromptLogResponse response = adminAiPromptService.getPromptLog(logId, user.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @DeleteMapping("/admin/ai-prompts/{logId}")
    public ResponseEntity<GeneralResponse<Void>> deleteAiPromptLog(
            @PathVariable UUID logId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        adminAiPromptService.delete(logId, user.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}
