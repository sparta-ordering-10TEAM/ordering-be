package com.sparta.ordering.global.config;

import com.sparta.ordering.restaurant.entity.Region;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.repository.RegionRepository;
import com.sparta.ordering.restaurant.repository.RestaurantCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 초기 기준 데이터 시더 (멱등).
 * ponytail: Flyway 미도입 상태의 임시 방편. 마이그레이션 도구 도입 시 시드 SQL로 이관.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final List<String> CATEGORY_CODES = List.of("한식", "중식", "분식", "치킨", "피자");

    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RegionRepository regionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedGwanghwamunRegions();
    }

    private void seedCategories() {
        for (String code : CATEGORY_CODES) {
            if (restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(code).isEmpty()) {
                restaurantCategoryRepository.save(RestaurantCategory.builder().code(code).build());
            }
        }
    }

    private void seedGwanghwamunRegions() {
        if (regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("서울특별시")) {
            return;
        }

        Region seoul = regionRepository.save(Region.builder().name("서울특별시").build());
        Region jongno = regionRepository.save(Region.builder().parent(seoul).name("종로구").build());
        regionRepository.save(Region.builder().parent(jongno).name("사직동").build());
    }
}
