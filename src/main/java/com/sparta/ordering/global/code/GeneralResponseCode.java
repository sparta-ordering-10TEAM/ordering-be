package com.sparta.ordering.global.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum GeneralResponseCode implements ApiResponseCode {
    // Common
    OK(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "성공적으로 생성되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 주문을 찾을 수 없습니다."),

    // Review
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 리뷰를 작성한 주문입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 리뷰를 찾을 수 없습니다."),
    ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "주문이 아직 완료되지 않았습니다."),

    // Restaurant
    RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 가게를 찾을 수 없습니다."),
    RESTAURANT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 카테고리를 찾을 수 없습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 상품을 찾을 수 없습니다."),

    //USER
    ALREADY_EXISTS_USER(HttpStatus.CONFLICT, "사용자가 이미 존재합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}