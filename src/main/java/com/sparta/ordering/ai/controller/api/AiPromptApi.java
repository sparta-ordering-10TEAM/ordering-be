package com.sparta.ordering.ai.controller.api;

import com.sparta.ordering.ai.dto.AiPromptLogResponse;
import com.sparta.ordering.ai.dto.GenerateProductDescriptionRequest;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.dto.GeneralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.UUID;

@Tag(name = "AI Prompt", description = "AI 프롬프트 생성 및 감사 로그 관리 API")
@RequestMapping("/api")
public interface AiPromptApi {

    @Operation(
            summary = "AI 상품 설명 생성",
            description = "프롬프트를 입력받아 AI(Gemini)를 통해 상품 설명 문구를 생성하여 직접 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/products/generate-description")
    ResponseEntity<GeneralResponse<String>> generateProductDescription(
            @RequestBody @Valid GenerateProductDescriptionRequest request
    );

    @Operation(
            summary = "[관리자] AI 프롬프트 로그 목록 조회",
            description = "생성된 전체 AI 프롬프트 및 응답 로그 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping("/admin/ai-prompts")
    ResponseEntity<GeneralResponse<Page<AiPromptLogResponse>>> searchAiPromptLogs(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "[관리자] AI 프롬프트 로그 단건 조회",
            description = "특정 AI 프롬프트 로그의 상세 내역을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping("/admin/ai-prompts/{logId}")
    ResponseEntity<GeneralResponse<AiPromptLogResponse>> getAiPromptLog(
            @PathVariable UUID logId,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "[관리자] AI 프롬프트 로그 삭제",
            description = "특정 AI 프롬프트 로그를 삭제(Soft Delete) 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @DeleteMapping("/admin/ai-prompts/{logId}")
    ResponseEntity<GeneralResponse<Void>> deleteAiPromptLog(
            @PathVariable UUID logId,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
