package com.sparta.ordering.auth.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sparta.ordering.auth.dto.TokenRotationResult;
import com.sparta.ordering.auth.security.JwtBlackList;
import com.sparta.ordering.auth.security.properties.JwtProperties;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtSessionServiceTest {

    private JwtSessionService jwtSessionService;

    @Mock
    private JwtSessionRepository jwtSessionRepository;
    @Mock
    private JwtBlackList jwtBlackList;
    @Mock
    private UserRepository userRepository;

    private JwtProperties jwtProperties;

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userName("testname")
                .nickName("테스트닉네임")
                .email("test@test.com")
                .phoneNumber("010-0000-0000")
                .role(Role.CUSTOMER)
                .password("testpassword")
                .locked(false)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        JwtProperties.TokenConfig accessTokenConfig = new JwtProperties.TokenConfig(3600L);
        JwtProperties.TokenConfig refreshTokenConfig = new JwtProperties.TokenConfig(7200L);
        jwtProperties = new JwtProperties(
                "test-issuer",
                "my-test-secret-key-my-test-secret-key",
                accessTokenConfig,
                refreshTokenConfig
        );

        jwtSessionService = new JwtSessionService(
                jwtSessionRepository,
                jwtProperties,
                userRepository,
                jwtBlackList
        );
    }

    @Test
    void JwtSession_생성_성공() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(jwtSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Instant before = Instant.now();
        JwtSession session = jwtSessionService.createJwtSession(userId);

        // then
        assertEquals(userId, session.getUserId());
        assertNotNull(session.getAccessToken());
        assertNotNull(session.getRefreshToken());
        long actualValidity = session.getExpirationTime().getEpochSecond() - before.getEpochSecond();
        long expectedValidity = jwtProperties.getAccessToken().getValiditySeconds();
        assertTrue(actualValidity <= expectedValidity && actualValidity >= expectedValidity - 2);
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void isValidToken_returnsTrue() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(jwtSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtBlackList.existsInBlacklist(anyString())).thenReturn(false);

        JwtSession session = jwtSessionService.createJwtSession(userId);
        String accessToken = session.getAccessToken();

        // when
        boolean result = jwtSessionService.isValidToken(accessToken);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("만료된 토큰에 대해 유효하지 않음 검증")
    void isValidToken_returnsFalse_forExpiredToken() {
        // given
        Instant issuedAt = Instant.now().minusSeconds(3600);
        Instant expiredAt = Instant.now().minusSeconds(1800);

        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiredAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // when
        boolean result = jwtSessionService.isValidToken(expiredToken);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("리프레시 토큰으로 토큰 무효화")
    void invalidateToken_byRefreshToken_deletesSessionAndBlacklistsToken() {
        String refreshToken = "refresh-token";
        String accessToken = "access-token";
        Instant expirationTime = Instant.now().plusSeconds(3600);

        JwtSession session = new JwtSession(userId, accessToken, refreshToken, expirationTime);
        when(jwtSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(session));

        jwtSessionService.invalidateToken(refreshToken);

        verify(jwtBlackList).addBlackList(accessToken, expirationTime);
        verify(jwtSessionRepository).delete(session);
    }

    @Test
    @DisplayName("userId로 토큰 무효화")
    void invalidateToken_byUserId_deletesSessionAndBlacklistsToken() {
        String refreshToken = "refresh-token";
        String accessToken = "access-token";
        Instant expirationTime = Instant.now().plusSeconds(3600);

        JwtSession session = new JwtSession(userId, accessToken, refreshToken, expirationTime);
        when(jwtSessionRepository.findByUserId(userId)).thenReturn(Optional.of(session));

        jwtSessionService.invalidateToken(userId);

        verify(jwtBlackList).addBlackList(accessToken, expirationTime);
        verify(jwtSessionRepository).delete(session);
    }

    @Test
    void 토큰_재발급_성공() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(jwtBlackList.existsInBlacklist(anyString())).thenReturn(false);

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.getRefreshToken().getValiditySeconds());
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        String refreshToken = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("type", "REFRESH")
                .claim("userId", user.getId().toString())
                .claim("name", user.getUserName())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        JwtSession session = new JwtSession(userId, "oldAccess", refreshToken, expiration);
        when(jwtSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(session));

        // when
        TokenRotationResult result = jwtSessionService.rotateToken(refreshToken);

        // then
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        verify(jwtBlackList).addBlackList(eq("oldAccess"), any());
    }

    @Test
    void 토큰에서_유저_정보_추출_성공() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(jwtSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JwtSession session = jwtSessionService.createJwtSession(userId);

        // when
        UUID extracted = jwtSessionService.extractUserId(session.getAccessToken());

        // then
        assertEquals(userId, extracted);
    }

    @Test
    void 리프레시토큰으로_액세스토큰_조회_성공() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(jwtSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        JwtSession session = jwtSessionService.createJwtSession(userId);
        String refreshToken = session.getRefreshToken();
        when(jwtSessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(session));

        // when
        String accessToken = jwtSessionService.findAccessToken(refreshToken);

        // then
        assertEquals(accessToken, session.getAccessToken());
    }
}