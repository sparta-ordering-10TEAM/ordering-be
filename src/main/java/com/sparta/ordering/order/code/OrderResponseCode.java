package com.sparta.ordering.order.code;

import com.sparta.ordering.global.code.ApiResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderResponseCode implements ApiResponseCode {
    ORDER_USER_REQUIRED(HttpStatus.BAD_REQUEST, "주문자는 필수입니다."),
    ORDER_ONLY_CUSTOMER(HttpStatus.FORBIDDEN, "고객만 주문할 수 있습니다."),
    ORDER_RESTAURANT_REQUIRED(HttpStatus.BAD_REQUEST, "주문 식당은 필수입니다."),
    ORDER_TOTAL_PRICE_INVALID(HttpStatus.BAD_REQUEST, "주문 금액이 올바르지 않습니다."),
    ORDER_NUMBER_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문번호 생성에 실패했습니다."),

    // 다른 서비스 에러코드
    ORDER_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 상품을 찾을 수 없습니다."),

    ORDER_RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "식당을 찾을 수 없습니다."),

    ORDER_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문자를 찾을 수 없습니다.");


    private final HttpStatus status;
    private final String message;
}