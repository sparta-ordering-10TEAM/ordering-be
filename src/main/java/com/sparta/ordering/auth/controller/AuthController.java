package com.sparta.ordering.auth.controller;

import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.auth.service.AuthService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtSessionService jwtSessionService;

    // 액세스 토큰 조회
    @GetMapping("/me")
    public ResponseEntity<GeneralResponse<String>> getMe(@CookieValue(value = "refresh_token") Cookie refreshToken) {
        String accessToken = jwtSessionService.findAccessToken(refreshToken.getValue());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, accessToken);
    }
}
