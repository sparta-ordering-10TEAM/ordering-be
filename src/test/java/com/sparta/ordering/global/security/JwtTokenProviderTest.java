package com.sparta.ordering.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sparta.ordering.user.entity.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String TEST_USER_NAME = "Test";
    private static final Role TEST_USER_ROLE = Role.CUSTOMER;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
        long accessExpiration = 3600000;
        long refreshExpiration = 604800000;
        jwtTokenProvider = new JwtTokenProvider(secret, accessExpiration, refreshExpiration);
    }

    @Test
    @DisplayName("Access Token을 생성하고 정보를 추출할 수 있다")
    public void generateAndParseAccessToken() {
        //when
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, TEST_USER_NAME, TEST_USER_ROLE);

        //then
        assertNotNull(token);
        assertEquals(TEST_USER_ID, jwtTokenProvider.getUserId(token));
        assertEquals(Role.CUSTOMER, jwtTokenProvider.getRole(token));
        assertEquals("ACCESS", jwtTokenProvider.getTokenType(token));
    }

    @Test
    @DisplayName("Refresh Token을 생성하고 정보를 추출할 수 있다")
    public void generateAndParseRefreshToken() {
        //when
        String token = jwtTokenProvider.generateRefreshToken(TEST_USER_ID, TEST_USER_NAME, TEST_USER_ROLE);

        //then
        assertNotNull(token);
        assertEquals(TEST_USER_ID, jwtTokenProvider.getUserId(token));
        assertEquals(Role.CUSTOMER, jwtTokenProvider.getRole(token));
        assertEquals("REFRESH", jwtTokenProvider.getTokenType(token));
    }

    @Test
    @DisplayName("유효한 토큰은 검증에 성공한다")
    public void validateValidToken() {
        //when
        String token = jwtTokenProvider.generateAccessToken(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "test2", Role.CUSTOMER);

        //then
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("잘못된 토큰은 검증에 실패한다")
    void validateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void validateExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm",
                -1000, -1000);

        String token = shortLivedProvider.generateAccessToken(TEST_USER_ID, TEST_USER_NAME, TEST_USER_ROLE);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 검증에 실패한다")
    void validateTokenWithDifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "other-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm",
                3600000, 604800000);

        String token = otherProvider.generateAccessToken(TEST_USER_ID, TEST_USER_NAME, TEST_USER_ROLE);

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}