package com.sparta.ordering.user.controller.api;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "관리자 - 회원 관리", description = "관리자 관련 API")
@RequestMapping("/api/admin/users")
public interface AdminApi {

    @Operation(summary = "계정 잠금 상태 변경", description = "[MANAGER, MASTER 기능] 사용자 계정을 잠급니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "계정 잠금 변경 성공",
                    content = @Content(schema = @Schema(implementation = UUID.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "계정 잠금 변경 실패 (사용자 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping
    ResponseEntity<GeneralResponse<UUID>> lock(@PathVariable UUID userId);

    @Operation(summary = "계정 잠금 상태 변경", description = "[MANAGER, MASTER 기능] 사용자 계정을 잠금 해제합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "계정 잠금 해제 변경 성공",
                    content = @Content(schema = @Schema(implementation = UUID.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "계정 잠금 해제 변경 실패 (사용자 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping
    ResponseEntity<GeneralResponse<UUID>> unlock(@PathVariable UUID userId);


}
