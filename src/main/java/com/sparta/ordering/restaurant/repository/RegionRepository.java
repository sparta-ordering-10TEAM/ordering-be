package com.sparta.ordering.restaurant.repository;

import com.sparta.ordering.restaurant.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
}
