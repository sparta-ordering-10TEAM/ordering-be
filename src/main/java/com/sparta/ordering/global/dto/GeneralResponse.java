package com.sparta.ordering.global.dto;

import com.sparta.ordering.global.code.ApiResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GeneralResponse<T> {
    private Integer status;
    private T data;

    public static <T> ResponseEntity<GeneralResponse<T>> toResponseEntity(ApiResponseCode responseCode, T data) {
        return ResponseEntity.status(responseCode.getStatus())
                .body(fromData(responseCode, data));
    }

    private static <T> GeneralResponse<T> fromData(ApiResponseCode responseCode, T data) {
        return GeneralResponse.<T>builder()
                .data(data)
                .status(responseCode.getStatus().value())
                .build();
    }
}
