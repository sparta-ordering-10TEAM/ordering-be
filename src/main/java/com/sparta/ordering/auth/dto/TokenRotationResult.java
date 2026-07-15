package com.sparta.ordering.auth.dto;

public record TokenRotationResult(
        String accessToken,
        String refreshToken
) {

}
