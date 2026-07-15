package com.sparta.ordering.auth.security.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
public class AuthorizationTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String OWNER_NAME = "authowner";
    private static final String CUSTOMER_NAME = "authcustomer";
    private static final String MASTER_NAME = "authmaster";
    private static final String PASSWORD = "Test1234!";

    @BeforeEach
    void setUp() {
        userRepository.findByUserNameAndDeletedAtIsNull(OWNER_NAME).ifPresent(userRepository::delete);
        userRepository.findByUserNameAndDeletedAtIsNull(CUSTOMER_NAME).ifPresent(userRepository::delete);
        userRepository.findByUserNameAndDeletedAtIsNull(MASTER_NAME).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .userName(OWNER_NAME)
                .nickName("인가테스트점주")
                .email("authowner@test.com")
                .phoneNumber("010-1111-0001")
                .role(Role.OWNER)
                .password(passwordEncoder.encode(PASSWORD))
                .locked(false)
                .build());

        userRepository.save(User.builder()
                .userName(CUSTOMER_NAME)
                .nickName("인가테스트고객")
                .email("authcustomer@test.com")
                .phoneNumber("010-2222-0001")
                .role(Role.CUSTOMER)
                .password(passwordEncoder.encode(PASSWORD))
                .locked(false)
                .build());

        userRepository.save(User.builder()
                .userName(MASTER_NAME)
                .nickName("인가테스트마스터")
                .email("authmaster@test.com")
                .phoneNumber("010-3333-0001")
                .role(Role.MASTER)
                .password(passwordEncoder.encode(PASSWORD))
                .locked(false)
                .build());
    }

    @AfterEach
    void tearDown() {
        userRepository.findByUserNameAndDeletedAtIsNull(OWNER_NAME).ifPresent(userRepository::delete);
        userRepository.findByUserNameAndDeletedAtIsNull(CUSTOMER_NAME).ifPresent(userRepository::delete);
        userRepository.findByUserNameAndDeletedAtIsNull(MASTER_NAME).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("CUSTOMER은 권한을 수정할 수 없다: hasRole('MASTER') 테스트")
    void updateRole_customer_returns_403() throws Exception {
        String accessToken = login(CUSTOMER_NAME, PASSWORD);
        UUID targetUserId = userRepository.findByUserNameAndDeletedAtIsNull(OWNER_NAME)
                .orElseThrow().getId();

        String body = objectMapper.writeValueAsString(Map.of("role", "OWNER"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users/" + targetUserId + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("MASTER은 권한을 수정할 수 있다: hasRole('MASTER') 테스트")
    void updateRole_master_passes_authorization() throws Exception {
        String accessToken = login(MASTER_NAME, PASSWORD);
        UUID targetUserId = userRepository.findByUserNameAndDeletedAtIsNull(OWNER_NAME)
                .orElseThrow().getId();

        String body = objectMapper.writeValueAsString(Map.of("role", "OWNER"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users/" + targetUserId + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    private String login(String userName, String password) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = objectMapper.writeValueAsString(Map.of("userName", userName, "password", password));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/sign-in",
                new HttpEntity<>(body, headers),
                String.class
        );

        log.debug("login response status: {}", response.getStatusCode());
        return objectMapper.readTree(response.getBody()).path("data").path("accessToken").asText();
    }

    private HttpEntity<Void> bearerEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }
}