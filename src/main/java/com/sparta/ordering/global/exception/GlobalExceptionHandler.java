package com.sparta.ordering.global.exception;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;


@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        String message = e.getMessage();
        GeneralResponseCode code = GeneralResponseCode.INVALID_REQUEST;

        // Review 생성 중복 조건
        if (message.contains("uk_review_order_customer")) {
            code = GeneralResponseCode.ALREADY_REVIEWED;
        }

        // Cart 생성 경합
        if (message.contains("uk_cart_user")) {
            code = GeneralResponseCode.CART_CREATE_CONFLICT;
        }

        // CartItem 추가 경합
        if (message.contains("uk_cart_item_cart_product")) {
            code = GeneralResponseCode.CART_ITEM_ADD_CONFLICT;
        }

        return ErrorResponse.toResponseEntity(code, null);
    }

    @ExceptionHandler(PropertyReferenceException.class) // Pageable 잘못된 Sort 값에 대한 예외처리
    public ResponseEntity<ErrorResponse> handlePropertyReferenceException(
            PropertyReferenceException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(GeneralResponseCode.INVALID_REQUEST, null);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiExceptionHandle(
            ApiException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(e.getResponseCode(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        BindingResult bindingResult = e.getBindingResult();

        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        HashMap<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ErrorResponse.toResponseEntity(GeneralResponseCode.INVALID_REQUEST, errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(AuthResponseCode.FORBIDDEN, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(GeneralResponseCode.INTERNAL_SERVER_ERROR, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(GeneralResponseCode.INVALID_REQUEST, null);
    }
}
