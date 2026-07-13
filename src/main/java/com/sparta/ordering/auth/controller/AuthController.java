package com.sparta.ordering.auth.controller;

import com.sparta.ordering.auth.dto.ResetPasswordRequest;
import com.sparta.ordering.auth.dto.TokenRotationResult;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.auth.service.AuthService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    //토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<GeneralResponse<String>> rotateToken(
            @CookieValue(value = "refresh_token") Cookie refreshToken,
            HttpServletResponse response) {
        TokenRotationResult result = jwtSessionService.rotateToken(refreshToken.getValue());
        // 리프레시 토큰을 쿠키로 다시 설정
        ResponseCookie cookie = ResponseCookie.from("refresh_token", result.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Strict")
                .maxAge(30 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result.accessToken());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GeneralResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }
}
