package com.sparta.ordering.user.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.controller.api.UserApi;
import com.sparta.ordering.user.dto.request.ChangePasswordRequest;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<GeneralResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest userCreateRequest) {
        UserResponse result = userService.create(userCreateRequest);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, result);
    }

    @Override
    @GetMapping("/{userId}/profiles")
    public ResponseEntity<GeneralResponse<ProfileResponse>> findProfile(@PathVariable UUID userId) {
        ProfileResponse result = userService.findProfile(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PatchMapping("/{userId}/profiles")
    public ResponseEntity<GeneralResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            @PathVariable UUID userId,
            @Valid @RequestPart(value = "request") ProfileUpdateRequest profileUpdateRequest,
            @RequestPart(value = "image", required = false) MultipartFile profileImage) {

        ProfileResponse result = userService.updateProfile(loginUser.getUserId(), userId, profileUpdateRequest, profileImage);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PatchMapping("/{userId}/password")
    public ResponseEntity<GeneralResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            @PathVariable UUID userId,
            @RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        userService.updatePassword(loginUser.getUserId(), userId, changePasswordRequest);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK,null);
    }

    @Override
    @DeleteMapping("/{userId}")
    public ResponseEntity<GeneralResponse<Void>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            @PathVariable UUID userId) {
        userService.deactivate(loginUser.getUserId(), userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK,null);
    }
}
