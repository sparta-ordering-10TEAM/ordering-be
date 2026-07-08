package com.sparta.ordering.global.exception;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> apiException(
            ApiException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(e.getResponseCode(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(
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
    public ResponseEntity<ErrorResponse> accessDeniedException(
            AccessDeniedException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(AuthResponseCode.FORBIDDEN, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exception(
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
