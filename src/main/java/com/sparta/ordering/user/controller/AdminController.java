package com.sparta.ordering.user.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.controller.api.AdminApi;
import com.sparta.ordering.user.dto.request.UserRoleUpdateRequest;
import com.sparta.ordering.user.dto.response.AdminUserDetailResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final AdminService adminService;

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @PatchMapping("/{userId}/lock")
    public ResponseEntity<GeneralResponse<UUID>> lock(@PathVariable UUID userId) {
        UUID result = adminService.lock(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @PatchMapping("/{userId}/unlock")
    public ResponseEntity<GeneralResponse<UUID>> unlock(@PathVariable UUID userId) {
        UUID result = adminService.unlock(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<GeneralResponse<UserResponse>> updateRole(@PathVariable UUID userId, @RequestBody UserRoleUpdateRequest userRoleUpdateRequest) {
        UserResponse result = adminService.updateRole(userId, userRoleUpdateRequest);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @GetMapping("{userId}")
    public ResponseEntity<GeneralResponse<AdminUserDetailResponse>> findUserDetail(@PathVariable UUID userId) {
        AdminUserDetailResponse result = adminService.findUserDetail(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @GetMapping
    public ResponseEntity<GeneralResponse<Page<AdminUserDetailResponse>>> searchUsers(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean locked,
            Pageable pageable) {
        Page<AdminUserDetailResponse> result = adminService.searchUsers(userName, role, locked, pageable);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }
}
