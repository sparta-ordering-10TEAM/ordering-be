package com.sparta.ordering.global.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum AuthResponseCode implements ApiResponseCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "인가되지 않은 요청입니다."),

    JWT_SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 만료되거나 로그아웃되었습니다."),

    INVALID_JWT(HttpStatus.UNAUTHORIZED, "JWT 토큰이 손상되었거나 유효하지 않습니다."),

    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "계정이 잠겨있어 로그인이 불가능합니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}