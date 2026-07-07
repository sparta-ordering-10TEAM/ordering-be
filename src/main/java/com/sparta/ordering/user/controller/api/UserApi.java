package com.sparta.ordering.user.controller.api;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.UUID;

@Tag(name = "프로필 관리", description = "프로필 관련 API")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(summary = "사용자 등록(회원가입)", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "사용자 등록(회원가입) 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "사용자 등록(회원가입) 실패",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PostMapping
    ResponseEntity<GeneralResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest);

    @Operation(summary = "프로필 조회", description = "특정 사용자의 프로필을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "프로필 조회 실패 (사용자 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @GetMapping
    ResponseEntity<GeneralResponse<ProfileResponse>> findProfile(@PathVariable UUID userId);

    @Operation(summary = "프로필 업데이트", description = "사용자의 프로필 정보를 업데이트합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "프로필 업데이트 실패 (사용자 없음)",
                content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping
    ResponseEntity<GeneralResponse<ProfileResponse>> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestPart("request") ProfileUpdateRequest profileUpdateRequest);


}
