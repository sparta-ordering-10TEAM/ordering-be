package com.sparta.ordering.global.security;

import com.sparta.ordering.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration:3600000}") long accessExpiration,
            @Value("${jwt.refresh-expiration:2592000000}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(UUID userId, String userName, Role userRole) {
        return generateToken(userId, userName, userRole, accessExpiration, "ACCESS");
    }

    public String generateRefreshToken(UUID userId, String userName, Role userRole) {
        return generateToken(userId, userName, userRole, refreshExpiration, "REFRESH");
    }

    public String getTokenType(String token) {
        return getClaims(token).get("tokenType", String.class);
    }

    public Role getRole(String token) {
        String roleStr = getClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String generateToken(UUID userId, String userName, Role userRole, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", userName)
                .claim("role", userRole)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }
}
