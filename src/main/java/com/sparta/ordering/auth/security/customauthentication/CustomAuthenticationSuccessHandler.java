package com.sparta.ordering.auth.security.customauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.session.JwtSession;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final JwtSessionService jwtSessionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        //인증정보 저장
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        //인증된 사용자 정보
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

        // 토큰 발급
        JwtSession jwtSession = jwtSessionService.createJwtSession(userId);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // response body에 액세스 토큰, response header에 리프레시 토큰 삽입
        Cookie refreshTokenCookie = new Cookie("refresh_token", jwtSession.getRefreshToken());
        refreshTokenCookie.setHttpOnly(false);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 쿠키 만료 시간 30일
        response.addCookie(refreshTokenCookie);
        objectMapper.writeValue(response.getWriter(), jwtSession.getAccessToken());
    }
}
