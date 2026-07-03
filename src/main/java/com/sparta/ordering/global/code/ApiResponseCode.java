package com.sparta.ordering.global.code;

import org.springframework.http.HttpStatus;

public interface ApiResponseCode {
    HttpStatus getStatus();

    String getMessage();
}
