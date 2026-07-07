package com.sparta.ordering.user.controller.api;

import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "프로필 관리", description = "프로필 관련 API")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(summary = "사용자 등록(회원가입)", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 등록(회원가입) 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "사용자 등록(회원가입) 실패"
                  //  content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PostMapping
    ResponseEntity<GeneralResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest);
}
