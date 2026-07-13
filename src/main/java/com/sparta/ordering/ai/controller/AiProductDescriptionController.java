package com.sparta.ordering.ai.controller;

import com.sparta.ordering.ai.controller.api.AiProductDescriptionApi;
import com.sparta.ordering.ai.dto.AiProductDescriptionResponse;
import com.sparta.ordering.ai.dto.GenerateAiProductDescriptionRequest;
import com.sparta.ordering.ai.dto.UpdateAiProductDescriptionRequest;
import com.sparta.ordering.ai.facade.AiProductDescriptionFacade;
import com.sparta.ordering.ai.service.AdminAiProductDescriptionService;
import com.sparta.ordering.ai.service.AiProductDescriptionService;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.entity.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
public class AiProductDescriptionController implements AiProductDescriptionApi {
    private final AiProductDescriptionService aiProductDescriptionService;
    private final AiProductDescriptionFacade aiProductDescriptionFacade;
    private final AdminAiProductDescriptionService adminAiProductDescriptionService;

    @Override
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/products/{productId}/ai-descriptions")
    public ResponseEntity<GeneralResponse<Page<AiProductDescriptionResponse>>> searchAiProductDescription(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        boolean isAdmin = isAdmin(authentication);

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                isAdmin ? adminAiProductDescriptionService.search(productId, user.getUserId(), pageable)
                        : aiProductDescriptionService.search(productId, user.getUserId(), pageable)
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/ai-descriptions/{aiDescriptionId}")
    public ResponseEntity<GeneralResponse<AiProductDescriptionResponse>> getAiProductDescription(
            @PathVariable UUID aiDescriptionId,
            @AuthenticationPrincipal CustomUserDetails user,
            Authentication authentication
    ) {
        boolean isAdmin = isAdmin(authentication);

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                isAdmin ? adminAiProductDescriptionService.getAiProductDescription(aiDescriptionId, user.getUserId())
                        : aiProductDescriptionService.getAiProductDescription(aiDescriptionId, user.getUserId())
        );
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/products/{productId}/ai-descriptions")
    public ResponseEntity<GeneralResponse<UUID>> generateAiProductDescription(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid GenerateAiProductDescriptionRequest request
    ) {
        // Facade로 위임하여 트랜잭션 경계 분리 호출
        UUID aiDescriptionId = aiProductDescriptionFacade.generate(productId, user.getUserId(), request.prompt());

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.CREATED,
                aiDescriptionId
        );
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/ai-descriptions/{aiDescriptionId}")
    public ResponseEntity<GeneralResponse<Void>> updateAiProductDescription(
            @PathVariable UUID aiDescriptionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid UpdateAiProductDescriptionRequest request
    ) {
        aiProductDescriptionService.update(aiDescriptionId, user.getUserId(), request.description());

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                null
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @DeleteMapping("/ai-descriptions/{aiDescriptionId}")
    public ResponseEntity<GeneralResponse<Void>> deleteAiProductDescription(
            @PathVariable UUID aiDescriptionId,
            @AuthenticationPrincipal CustomUserDetails user,
            Authentication authentication
    ) {
        boolean isAdmin = isAdmin(authentication);

        if (isAdmin) {
            adminAiProductDescriptionService.delete(aiDescriptionId, user.getUserId());
        } else {
            aiProductDescriptionService.delete(aiDescriptionId, user.getUserId());
        }

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                null
        );
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(Role::valueOf)
                .anyMatch(Role::isAdmin);
    }
}