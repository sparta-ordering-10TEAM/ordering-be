package com.sparta.ordering.restaurant.dto;

import com.sparta.ordering.restaurant.entity.Region;

import java.util.UUID;

public record RegionResponse(
        UUID regionId,
        UUID parentId,
        String name,
        Integer depth
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getParent() != null ? region.getParent().getId() : null,
                region.getName(),
                region.getDepth()
        );
    }
}
