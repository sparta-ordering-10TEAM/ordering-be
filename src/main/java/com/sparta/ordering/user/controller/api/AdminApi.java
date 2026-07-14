package com.sparta.ordering.user.controller.api;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.dto.request.UserRoleUpdateRequest;
import com.sparta.ordering.user.dto.response.AdminUserDetailResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Operation(summary = "권한 수정", description = "[MASTER 기능] 사용자의 권한을 수정합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "권한 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "권한 수정 실패 (사용자 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping
    ResponseEntity<GeneralResponse<UserResponse>> updateRole(@PathVariable UUID userId,
                                                             @RequestBody UserRoleUpdateRequest userRoleUpdateRequest);

    @Operation(summary = "가게주인 승인", description = "[MANAGER, MASTER 기능] CUSTOMER 상태의 회원을 OWNER로 승인합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "가게주인 승인 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "가게주인 승인 실패 (CUSTOMER가 아닌 회원)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "가게주인 승인 실패 (사용자 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping
    ResponseEntity<GeneralResponse<UserResponse>> approveOwner(@PathVariable UUID userId,
                                                               UUID approveOwner);

    @Operation(summary = "회원 상세 조회", description = "관리자가 회원을 상세 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminUserDetailResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원 상세 조회 실패 (사용자 없음)"
            )
    })
    @GetMapping
    ResponseEntity<GeneralResponse<AdminUserDetailResponse>> findUserDetail(@PathVariable UUID userId);

    @Operation(summary = "계정 목록 조회", description = "관리자가 전체 계정 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "계정 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "계정 목록 조회 실패"
            )
    })
    @GetMapping
    ResponseEntity<GeneralResponse<Page<AdminUserDetailResponse>>> searchUsers(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean locked,
            Pageable pageable);
}
