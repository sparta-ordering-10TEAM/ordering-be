package com.sparta.ordering.auth.security.customauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(AuthResponseCode.LOGIN_FAILED.getStatus().value())
                .message(AuthResponseCode.LOGIN_FAILED.getMessage())
                .build();

        response.setStatus(AuthResponseCode.LOGIN_FAILED.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
