package com.sparta.ordering.user.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.controller.api.AdminApi;
import com.sparta.ordering.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<GeneralResponse<UUID>> lock(@PathVariable UUID userId) {
        UUID result = adminService.lock(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }

    @Override
    @PatchMapping("/{userId}/unlock")
    public ResponseEntity<GeneralResponse<UUID>> unlock(@PathVariable UUID userId) {
        UUID result = adminService.unlock(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }
}
