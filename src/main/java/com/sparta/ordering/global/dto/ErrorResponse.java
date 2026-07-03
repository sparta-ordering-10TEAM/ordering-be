package com.sparta.ordering.global.dto;

import com.sparta.ordering.global.code.ApiResponseCode;
import lombok.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private Integer status;
    private String message;
    private HashMap<String, String> errors;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ApiResponseCode responseCode, HashMap<String, String> errors) {
        return ResponseEntity.status(responseCode.getStatus())
                .body(ErrorResponse.fromData(
                        responseCode, errors));
    }

    private static ErrorResponse fromData(ApiResponseCode responseCode, HashMap<String, String> errors) {
        return ErrorResponse.builder()
                .errors(errors)
                .message(responseCode.getMessage())
                .status(responseCode.getStatus().value())
                .build();
    }
}
