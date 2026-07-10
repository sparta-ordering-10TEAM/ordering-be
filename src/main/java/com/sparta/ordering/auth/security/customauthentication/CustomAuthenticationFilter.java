package com.sparta.ordering.auth.security.customauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.dto.SignInRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " +
                    request.getMethod());
        }

        // Request Body에서 userName, password 추출
        try {
            SignInRequest signInRequest = objectMapper.readValue(request.getInputStream(), SignInRequest.class);
            String userName = signInRequest.userName();
            String password = signInRequest.password();

            //Authentication 객체 생성
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName, password);

            // request에서 IP 등을 추출해서 authenticationToken.setDetails()로 인증 객체에 부가 정보를 넣음
            setDetails(request, authenticationToken);

            return getAuthenticationManager().authenticate(authenticationToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
