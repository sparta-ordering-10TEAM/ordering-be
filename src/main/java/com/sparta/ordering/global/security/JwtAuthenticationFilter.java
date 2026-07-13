package com.sparta.ordering.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.ErrorResponse;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtSessionService jwtSessionService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolve(request);

        // 토큰 유효성 & 로그인 상태 검증
        if (token != null && jwtSessionService.isValidToken(token) && jwtSessionService.isSignedIn(token)) {
            //인증 처리
            UUID userId = jwtSessionService.extractUserId(token);

            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

            CustomUserDetails userDetails = new CustomUserDetails(
                    userId,
                    user.getUserName(),
                    user.getPassword(),
                    user.getRole(),
                    user.isLocked(),
                    user.getTempPasswordExpirationTime()
            );

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        }else if(token != null){
            // 잘못된 토큰 & 비로그인 상태 -> 401 응답
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .message("JwtValidationException")
                    .build();
            objectMapper.writeValue(response.getWriter(), errorResponse);
        }else {
            //토큰이 없는 경우
            // TODO: 인증이 필요없는 경로면 다음 필터 진행, 아니면 401 응답
            filterChain.doFilter(request, response);
        }

    }

    private String resolve(HttpServletRequest request) {
        // AUTHORIZATION 헤더에서 추출
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
