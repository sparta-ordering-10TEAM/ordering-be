package com.sparta.ordering.auth.security.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
public class AuthenticationTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    UUID userId;

    private static final String CUSTOMER_NAME = "authcustomer";
    private static final String PASSWORD = "Test1234!";

    @BeforeEach
    void setUp() {
        userRepository.findByUserNameAndDeletedAtIsNull(CUSTOMER_NAME).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .userName(CUSTOMER_NAME)
                .nickName("인가테스트고객")
                .email("authcustomer@test.com")
                .phoneNumber("010-2222-0001")
                .role(Role.CUSTOMER)
                .password(passwordEncoder.encode(PASSWORD))
                .locked(false)
                .build());

    }

    @AfterEach
    void tearDown() {
        userRepository.findByUserNameAndDeletedAtIsNull(CUSTOMER_NAME).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰이 응답 바디와 쿠키에 포함된다.")
    void SignInSuccessShouldReturnToken() {
        // given
        Map<String, String> requestBody = Map.of(
                "userName", CUSTOMER_NAME,
                "password", PASSWORD
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        // when
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/sign-in", request,
                String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();

        Optional<String> refreshCookie = cookies.stream()
                .filter(cookie -> cookie.startsWith("refresh_token="))
                .findFirst();

        assertThat(refreshCookie).isPresent();
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이 있을 때 토큰 재발급 요청으로 새로운 토큰을 받는다.")
    void refreshToken() throws Exception {
        // given - 로그인으로 리프레시 토큰 획득
        Map<String, String> loginBody = Map.of(
                "userName", CUSTOMER_NAME,
                "password", PASSWORD
        );
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/sign-in", new HttpEntity<>(loginBody, loginHeaders), String.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<String> setCookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull();

        String refreshTokenCookie = setCookies.stream()
                .filter(cookie -> cookie.startsWith("refresh_token="))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No refresh_token cookie found"));
        String refreshTokenPair = refreshTokenCookie.split(";", 2)[0]; // "refresh_token=<value>" 형태

        // when - 토큰 재발급 요청
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.set(HttpHeaders.COOKIE, refreshTokenPair);
        ResponseEntity<String> refreshResponse = restTemplate.postForEntity(
                "/api/auth/refresh", new HttpEntity<>(null, refreshHeaders), String.class
        );

        // then
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(refreshResponse.getBody());
        assertNotEquals(loginResponse.getBody(), refreshResponse.getBody());

        List<String> cookies = refreshResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();

        Optional<String> newRefreshCookie = cookies.stream()
                .filter(cookie -> cookie.startsWith("refresh_token="))
                .findFirst();

        assertThat(newRefreshCookie).isPresent();
        assertNotEquals(refreshTokenCookie, newRefreshCookie.get());
    }
}
