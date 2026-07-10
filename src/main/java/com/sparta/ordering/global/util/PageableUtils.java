package com.sparta.ordering.global.util;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public class PageableUtils {
    // 페이지 크기는 10/30/50만 허용, 그 외 요청은 기본값 10으로 고정
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Set<Integer> AVAILABLE_PAGE_SIZES = Set.of(10, 30, 50);


    public static Pageable normalizePageSize(Pageable pageable) {
        int pageSize = pageable.getPageSize();
        if (AVAILABLE_PAGE_SIZES.contains(pageSize)) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), DEFAULT_PAGE_SIZE, pageable.getSort());
    }

    // 정렬 기준 검증
    public static void validateSort(Pageable pageable, Set<String> allowedSortFields) {

        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !allowedSortFields.contains(order.getProperty()));

        if (invalid) {
            throw new ApiException(GeneralResponseCode.INVALID_REQUEST);
        }
    }
}
