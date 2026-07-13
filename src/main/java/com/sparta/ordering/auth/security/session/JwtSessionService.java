package com.sparta.ordering.auth.security.session;

import com.sparta.ordering.auth.dto.TokenRotationResult;
import com.sparta.ordering.auth.security.properties.JwtProperties;
import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtSessionService {
    private final JwtSessionRepository jwtSessionRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    /*getSigningKey() 메서드가 호출될 때마다 새로운 SecretKey 인스턴스를 생성하고 있어 성능에 영향을 줄 가능성 존재
    -> 서명키를 캐시하도록 수정:*/
    private SecretKey signingKey;
    private final UserRepository userRepository;

    public enum TokenType{
        ACCESS, REFRESH
    }

    @Transactional
    public JwtSession createJwtSession(UUID userId) {
        Instant now = clock.instant();
        Instant accessTokenExpirationTime = now
                .plusSeconds(jwtProperties.getAccessToken().getValiditySeconds());
        Instant refreshTokenExpirationTime = now
                .plusSeconds(jwtProperties.getRefreshToken().getValiditySeconds());

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        String accessToken = createTokenWithClaims(user, accessTokenExpirationTime, TokenType.ACCESS);
        String refreshToken = createTokenWithClaims(user, refreshTokenExpirationTime, TokenType.REFRESH);

        JwtSession jwtSession = jwtSessionRepository.save(
                new JwtSession(
                        userId,
                        accessToken,
                        refreshToken,
                        accessTokenExpirationTime
                )
        );
        return jwtSession;
    }

    // 토큰 유효성 검증
    public boolean isValidToken(String token) {
        try{
            JwtParser parser = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build();
            parser.parseSignedClaims(token).getPayload();

            return true;
        }catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    // 로그인 상태 확인
    public boolean isSignedIn(String token) {
        return jwtSessionRepository.existsByAccessToken(token);
    }

    // 토큰에서 사용자 ID 추출
    public UUID extractUserId(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build();

            Claims claims = parser
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.get("userId", String.class);
            return UUID.fromString(userId);
        } catch (JwtException e) {
            log.error("Failed to extract user ID from token", e);
            throw new ApiException(AuthResponseCode.INVALID_JWT);
        }catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in token subject", e);
            throw new ApiException(AuthResponseCode.INVALID_JWT);
        }
    }

    // 리프레스 토큰으로 강제 로그아웃
    @Transactional
    public void invalidateToken(String refreshToken) {
        jwtSessionRepository.findByRefreshToken(refreshToken)
                .ifPresentOrElse(jwtSession -> {
                   // TODO:토큰을 블랙리스트에 추가
                    jwtSessionRepository.delete(jwtSession);
                        },
                        ()->log.info("No active JwtSession found for refreshToken: {}", refreshToken)
                );
    }

    // userId로 강제 로그아웃
    @Transactional
    public void invalidateToken(UUID userId) {
        jwtSessionRepository.findByUserId(userId)
                .ifPresentOrElse(jwtSession -> {
                   // TODO:토큰을 블랙리스트에 추가
                    jwtSessionRepository.delete(jwtSession);
                        },
                        ()->log.info("No active JwtSession found for userId: {}", userId)
                );
    }

    // 리프레시 토큰으로 액세스 토큰 조회
    public String findAccessToken(String refreshToken) {
        JwtSession jwtSession = jwtSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ApiException(AuthResponseCode.JWT_SESSION_NOT_FOUND));
        return jwtSession.getAccessToken();
    }

    // 리프레시 토큰으로 액세스, 리프레시 토큰 재발급
    @Transactional
    public TokenRotationResult rotateToken(String refreshToken) {
        if (!isValidToken(refreshToken)) {
            throw new ApiException(AuthResponseCode.INVALID_JWT);
        }

        JwtSession jwtSession = jwtSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ApiException(AuthResponseCode.JWT_SESSION_NOT_FOUND));

        User user = userRepository.findByIdAndDeletedAtIsNull(jwtSession.getUserId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        Instant now = clock.instant();
        Instant newAccessTokenExpirationTime = now
                .plusSeconds(jwtProperties.getAccessToken().getValiditySeconds());
        Instant newRefreshTokenExpirationTime = now
                .plusSeconds(jwtProperties.getRefreshToken().getValiditySeconds());

        String newAccessToken = createTokenWithClaims(user, newAccessTokenExpirationTime, TokenType.ACCESS);
        String newRefreshToken = createTokenWithClaims(user, newRefreshTokenExpirationTime, TokenType.REFRESH);

        jwtSession.update(newAccessToken, newRefreshToken, newAccessTokenExpirationTime);
        return new TokenRotationResult(newAccessToken, newRefreshToken);
    }

    //토큰 생성
    private String createTokenWithClaims(User user, Instant expirationTime, TokenType tokenType) {
        Instant now = clock.instant();

        JwtBuilder builder = Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .issuer(jwtProperties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .claim("type", tokenType.name())
                .claim("userId", user.getId().toString())
                .claim("name",user.getUserName())
                .claim("role",user.getRole())
                .claim("email",user.getEmail());

        return builder.signWith(getSignKey(), Jwts.SIG.HS256)
                .compact();
    }

    //서명키 생성
    private SecretKey getSignKey() {
        if (this.signingKey == null) {
            byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return this.signingKey;
    }
}
