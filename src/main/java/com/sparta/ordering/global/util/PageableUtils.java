package com.sparta.ordering.global.util;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public class PageableUtils {

    // 정렬 기준 검증
    public static void validateSort(Pageable pageable, Set<String> allowedSortFields) {

        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !allowedSortFields.contains(order.getProperty()));

        if (invalid) {
            throw new ApiException(GeneralResponseCode.INVALID_REQUEST);
        }
    }
}
