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
    ORDER_TOTAL_PRICE_INVALID(HttpStatus.BAD_REQUEST, "주문 금액이 올바르지 않습니다."),
    ORDER_NUMBER_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문번호 생성에 실패했습니다."),
    ORDER_ONLY_CUSTOMER(HttpStatus.FORBIDDEN, "고객만 주문할 수 있습니다."),
    ORDER_RESTAURANT_NOT_OPEN(HttpStatus.BAD_REQUEST, "영업 중인 식당에만 주문할 수 있습니다."),
    ORDER_PRODUCT_RESTAURANT_MISMATCH(HttpStatus.BAD_REQUEST, "해당 식당의 상품만 주문할 수 있습니다."),

    // Review
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 리뷰를 작성한 주문입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 리뷰를 찾을 수 없습니다."),
    ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "주문이 아직 완료되지 않았습니다."),

    // Restaurant
    RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 가게를 찾을 수 없습니다."),
    RESTAURANT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 카테고리를 찾을 수 없습니다."),
    RESTAURANT_STATUS_INVALID(HttpStatus.BAD_REQUEST, "영업 상태가 빈 값입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 상품을 찾을 수 없습니다."),

    //USER
    USER_ALREADY_LOCKED(HttpStatus.CONFLICT, "이미 잠긴 계정입니다."),
    USER_ALREADY_UNLOCKED(HttpStatus.CONFLICT, "이미 잠금 해제된 계정입니다."),
    ALREADY_EXISTS_USER(HttpStatus.CONFLICT, "사용자가 이미 존재합니다."),
    ALREADY_EXISTS_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임 입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 사용자를 찾을 수 없습니다."),

    // AI Product Description
    AI_PRODUCT_DESCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 AI 상품 설명을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}