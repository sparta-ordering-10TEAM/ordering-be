package com.sparta.ordering.restaurant.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.dto.RegionCreateRequest;
import com.sparta.ordering.restaurant.dto.RegionResponse;
import com.sparta.ordering.restaurant.dto.RegionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Region", description = "지역 관련 API")
@RequestMapping("/api")
public interface RegionApi {

    @Operation(summary = "지역 목록 조회", description = "parentId 조건으로 지역 목록을 조회합니다. parentId가 없으면 최상위 지역을 반환합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "지역 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = RegionResponse.class)))
    })
    @GetMapping("/regions")
    ResponseEntity<GeneralResponse<List<RegionResponse>>> getRegions(
            @RequestParam(required = false) UUID parentId
    );

    @Operation(
            summary = "지역 생성",
            description = "MANAGER/MASTER가 새 지역을 등록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "지역 생성 성공",
                    content = @Content(schema = @Schema(implementation = RegionResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "지역 생성 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지역 생성 실패 (상위 지역/사용자 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "지역 생성 실패 (동일 계층 지역 이름 중복)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PostMapping("/regions")
    ResponseEntity<GeneralResponse<RegionResponse>> createRegion(
            @Valid @RequestBody RegionCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "지역 수정",
            description = "MANAGER/MASTER가 지역 이름을 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "지역 수정 성공",
                    content = @Content(schema = @Schema(implementation = RegionResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "지역 수정 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지역 수정 실패 (지역 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "지역 수정 실패 (동일 계층 지역 이름 중복)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PutMapping("/regions/{regionId}")
    ResponseEntity<GeneralResponse<RegionResponse>> updateRegion(
            @PathVariable UUID regionId,
            @Valid @RequestBody RegionUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "지역 삭제",
            description = "MANAGER/MASTER가 지역을 논리 삭제합니다. 하위 지역이 있으면 삭제할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "지역 삭제 성공",
                    content = @Content(schema = @Schema())),
            @ApiResponse(
                    responseCode = "400",
                    description = "지역 삭제 실패 (하위 지역 존재)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "지역 삭제 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지역 삭제 실패 (지역 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @DeleteMapping("/regions/{regionId}")
    ResponseEntity<GeneralResponse<Void>> deleteRegion(
            @PathVariable UUID regionId,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
