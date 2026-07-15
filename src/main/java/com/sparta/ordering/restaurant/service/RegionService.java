package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.RegionCreateRequest;
import com.sparta.ordering.restaurant.dto.RegionResponse;
import com.sparta.ordering.restaurant.dto.RegionUpdateRequest;
import com.sparta.ordering.restaurant.entity.Region;
import com.sparta.ordering.restaurant.repository.RegionRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionService {

    private static final Set<Role> ADMIN_ROLES = Set.of(Role.MASTER, Role.MANAGER);

    private final RegionRepository regionRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> getRegions(UUID parentId) {
        List<Region> regions = (parentId == null)
                ? regionRepository.findByParentIsNullAndDeletedAtIsNull()
                : regionRepository.findByParent_IdAndDeletedAtIsNull(parentId);
        return regions.stream().map(RegionResponse::from).toList();
    }

    @Transactional
    public RegionResponse createRegion(RegionCreateRequest request, UUID userId) {

        validateAdminPermission(userId);

        Region parent = (request.parentId() != null) ? getActiveRegion(request.parentId()) : null;

        boolean duplicated = (parent == null)
                ? regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull(request.name())
                : regionRepository.existsByParent_IdAndNameAndDeletedAtIsNull(parent.getId(), request.name());
        if (duplicated){
            throw new ApiException(GeneralResponseCode.REGION_ALREADY_EXISTS);
        }

        Region region = Region.builder()
                .parent(parent)
                .name(request.name())
                .build();

        return RegionResponse.from(regionRepository.save(region));
    }

    @Transactional
    public RegionResponse updateRegion(UUID regionId, RegionUpdateRequest request, UUID userId) {
        validateAdminPermission(userId);

        Region region = getActiveRegion(regionId);

        UUID parentId = (region.getParent() != null) ? region.getParent().getId() : null;
        boolean duplicated = (parentId == null)
                ? regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull(request.name())
                : regionRepository.existsByParent_IdAndNameAndDeletedAtIsNull(parentId, request.name());
        if (duplicated && !region.getName().equals(request.name())){
            throw new ApiException(GeneralResponseCode.REGION_ALREADY_EXISTS);
        }

        region.rename(request.name());

        return RegionResponse.from(region);
    }

    @Transactional
    public void deleteRegion(UUID regionId, UUID userId) {
        validateAdminPermission(userId);

        Region region = getActiveRegion(regionId);

        if (regionRepository.existsByParent_IdAndDeletedAtIsNull(regionId)) {
            throw new ApiException(GeneralResponseCode.REGION_HAS_CHILDREN);
        }

        if (restaurantRepository.existsByRegion_IdAndDeletedAtIsNull(regionId)) {
            throw new ApiException(GeneralResponseCode.REGION_IN_USE);
        }

        region.softDelete(userId);
    }

    private void validateAdminPermission(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!ADMIN_ROLES.contains(user.getRole()))
            throw new ApiException(AuthResponseCode.FORBIDDEN);
    }

    private Region getActiveRegion(UUID regionId) {
        return regionRepository.findByIdAndDeletedAtIsNull(regionId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.REGION_NOT_FOUND));
    }
}