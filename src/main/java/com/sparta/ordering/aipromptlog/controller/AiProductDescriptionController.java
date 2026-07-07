package com.sparta.ordering.aipromptlog.controller;

import com.sparta.ordering.aipromptlog.dto.AiProductDescriptionResponse;
import com.sparta.ordering.aipromptlog.dto.GenerateAiProductDescriptionRequest;
import com.sparta.ordering.aipromptlog.dto.UpdateAiProductDescriptionRequest;
import com.sparta.ordering.aipromptlog.service.AiProductDescriptionService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AiProductDescriptionController {
    private final AiProductDescriptionService aiProductDescriptionService;

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/products/{productId}/ai-descriptions")
    public ResponseEntity<GeneralResponse<Page<AiProductDescriptionResponse>>> searchAiProductDescription(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                aiProductDescriptionService.search(productId, userId, pageable)
        );
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/products/{productId}/ai-descriptions")
    public ResponseEntity<GeneralResponse<Void>> generateAiProductDescription(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid GenerateAiProductDescriptionRequest request
    ) {
        aiProductDescriptionService.generate(productId, userId, request.prompt());

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                null
        );
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/ai-descriptions/{aiDescriptionId}")
    public ResponseEntity<GeneralResponse<Void>> updateAiProductDescription(
            @PathVariable UUID aiDescriptionId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid UpdateAiProductDescriptionRequest request
    ) {
        aiProductDescriptionService.update(aiDescriptionId, userId, request.description());

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                null
        );
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/ai-descriptions/{aiDescriptionId}")
    public ResponseEntity<GeneralResponse<Void>> deleteAiProductDescription(
            @PathVariable UUID aiDescriptionId,
            @AuthenticationPrincipal UUID userId
    ) {
        aiProductDescriptionService.delete(aiDescriptionId, userId);

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                null
        );
    }
}