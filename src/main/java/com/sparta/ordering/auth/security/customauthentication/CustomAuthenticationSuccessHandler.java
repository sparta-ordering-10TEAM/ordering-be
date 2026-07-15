package com.sparta.ordering.auth.security.customauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.dto.SignInResponse;
import com.sparta.ordering.auth.security.session.JwtSession;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

        // 기존에 로그인된 계정이 있을 경우, 강제로 로그아웃 처리
        jwtSessionService.invalidateToken(userId);

        // 토큰 발급
        JwtSession jwtSession = jwtSessionService.createJwtSession(userId);

        Cookie refreshTokenCookie = new Cookie("refresh_token", jwtSession.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60);
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);

        GeneralResponse<SignInResponse> body = GeneralResponse.<SignInResponse>builder()
                .status(GeneralResponseCode.OK.getStatus().value())
                .data(new SignInResponse(jwtSession.getAccessToken()))
                .build();

        response.setStatus(GeneralResponseCode.OK.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
