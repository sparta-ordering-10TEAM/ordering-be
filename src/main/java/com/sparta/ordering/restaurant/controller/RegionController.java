package com.sparta.ordering.restaurant.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.controller.api.RegionApi;
import com.sparta.ordering.restaurant.dto.RegionCreateRequest;
import com.sparta.ordering.restaurant.dto.RegionResponse;
import com.sparta.ordering.restaurant.dto.RegionUpdateRequest;
import com.sparta.ordering.restaurant.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RegionController implements RegionApi {

    private final RegionService regionService;

    @Override
    @GetMapping("/regions")
    public ResponseEntity<GeneralResponse<List<RegionResponse>>> getRegions(
            @RequestParam(required = false) UUID parentId) {

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                regionService.getRegions(parentId));
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @PostMapping("/regions")
    public ResponseEntity<GeneralResponse<RegionResponse>> createRegion(
            @Valid @RequestBody RegionCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                regionService.createRegion(request, user.getUserId()));
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @PutMapping("/regions/{regionId}")
    public ResponseEntity<GeneralResponse<RegionResponse>> updateRegion(
            @PathVariable UUID regionId,
            @Valid @RequestBody RegionUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return GeneralResponse.toResponseEntity(
                GeneralResponseCode.OK,
                regionService.updateRegion(regionId, request, user.getUserId()));
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @DeleteMapping("/regions/{regionId}")
    public ResponseEntity<GeneralResponse<Void>> deleteRegion(
            @PathVariable UUID regionId,
            @AuthenticationPrincipal CustomUserDetails user) {

        regionService.deleteRegion(regionId, user.getUserId());

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}
