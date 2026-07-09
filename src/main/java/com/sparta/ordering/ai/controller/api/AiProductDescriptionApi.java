package com.sparta.ordering.ai.controller.api;

import com.sparta.ordering.ai.dto.AiProductDescriptionResponse;
import com.sparta.ordering.ai.dto.GenerateAiProductDescriptionRequest;
import com.sparta.ordering.ai.dto.UpdateAiProductDescriptionRequest;
import com.sparta.ordering.global.dto.GeneralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "AI Product Description", description = "AI 상품 설명 관리 API")
public interface AiProductDescriptionApi {

    @Operation(
            summary = "AI 상품 설명 목록 조회",
            description = "특정 상품의 AI 상품 설명 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Page<AiProductDescriptionResponse>>> searchAiProductDescription(
            UUID productId,
            UUID userId,
            Pageable pageable
    );

    @Operation(
            summary = "AI 상품 설명 생성",
            description = "입력된 프롬프트를 기반으로 AI 상품 설명을 자동 생성하고 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Void>> generateAiProductDescription(
            UUID productId,
            UUID userId,
            @Valid GenerateAiProductDescriptionRequest request
    );

    @Operation(
            summary = "AI 상품 설명 수정",
            description = "특정 AI 상품 설명의 생성 내용을 직접 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Void>> updateAiProductDescription(
            UUID aiDescriptionId,
            UUID userId,
            @Valid UpdateAiProductDescriptionRequest request
    );

    @Operation(
            summary = "AI 상품 설명 삭제",
            description = "생성된 AI 상품 설명을 삭제(Soft Delete)합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<GeneralResponse<Void>> deleteAiProductDescription(
            UUID aiDescriptionId,
            UUID userId
    );
}
