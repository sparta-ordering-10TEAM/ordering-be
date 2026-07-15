package com.sparta.ordering.global.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExternalResponseCode implements ApiResponseCode {
    GEMINI_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 AI API 호출 도중 오류가 발생했습니다."),
    GEMINI_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "외부 AI API 응답 형식이 유효하지 않습니다."),

    PG_APPROVAL_ERROR(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
    PG_CANCEL_ERROR(HttpStatus.BAD_GATEWAY, "결제 취소에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
