package com.sparta.ordering.user.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.controller.api.AdminApi;
import com.sparta.ordering.user.dto.request.UserLockUpdateRequest;
import com.sparta.ordering.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final AdminService adminService;

    @Override
    @PatchMapping("/{userId}/lock")
    public ResponseEntity<GeneralResponse<UUID>> updateLock(@PathVariable UUID userId,
                                                            @RequestBody UserLockUpdateRequest userLockUpdateRequest) {
        UUID result = adminService.updateLock(userId, userLockUpdateRequest);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }
}
