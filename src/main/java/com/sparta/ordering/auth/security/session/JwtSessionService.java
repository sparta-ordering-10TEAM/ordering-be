package com.sparta.ordering.auth.security.session;

import com.sparta.ordering.auth.security.properties.JwtProperties;
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
        Instant accessTokenExpirationTime = Instant.now()
                .plusSeconds(jwtProperties.getAccessToken().getValiditySeconds());
        Instant refreshTokenExpirationTime = Instant.now()
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


    // 토큰 유효성 인증
    public boolean validateToken(String token) {
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
            throw new IllegalArgumentException("Invalid JWT token", e);
        }catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in token subject", e);
            throw new IllegalArgumentException("Invalid user ID format in token", e);
        }
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
            return Keys.hmacShaKeyFor(keyBytes);
        }
        return this.signingKey;
    }
}
