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

    // Payment
    PAYMENT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 결제 내역을 찾을 수 없습니다."),
    PAYMENT_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 처리 중이거나 완료된 결제 요청입니다."),
    PAYMENT_INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "현재 상태에서는 요청을 처리할 수 없습니다"),

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

    // Cart
    CART_DIFFERENT_RESTAURANT(HttpStatus.BAD_REQUEST, "장바구니에는 같은 식당의 상품만 담을 수 있습니다."),
    CART_ITEM_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "수량은 1개 이상 99개 이하여야 합니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 장바구니 상품을 찾을 수 없습니다"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 장바구니를 찾을 수 없습니다." ),
    CART_CREATE_CONFLICT(HttpStatus.CONFLICT, "장바구니를 생성하는 중 문제가 발생했습니다. 다시 시도해주세요."),
    CART_ITEM_ADD_CONFLICT(HttpStatus.CONFLICT, "장바구니에 상품을 담는 중 문제가 발생했습니다. 다시 시도해주세요."),

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