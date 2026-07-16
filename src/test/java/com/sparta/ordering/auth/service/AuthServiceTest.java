package com.sparta.ordering.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sparta.ordering.auth.dto.ResetPasswordRequest;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Spy
    private SecureRandom secureRandom = new SecureRandom();

    UUID userId;
    User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .userName("test")
                .nickName("testname")
                .email("test@test.mail")
                .phoneNumber("010-0000-0000")
                .password(passwordEncoder.encode("testpw"))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    void 비밀번호_초기화_성공() {
        // given
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest("test@test.mail");
        when(userRepository.findByEmailAndDeletedAtIsNull("test@test.mail")).thenReturn(Optional.of(user));
        doNothing().when(emailService).sendTempPasswordEmail(any(), any());

        // when
        authService.resetPassword(resetPasswordRequest);

        // then
        verify(emailService).sendTempPasswordEmail(any(), any());
        assertTrue(!passwordEncoder.matches("testpw", user.getPassword()));
        assertNotNull(user.getTempPasswordExpirationTime());
    }

    @Test
    void 비밀번호_초기화_실패() {
        // given
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest("test@test.mail");
        when(userRepository.findByEmailAndDeletedAtIsNull("test@test.mail")).thenReturn(Optional.empty());

        // when & then
        ApiException ex = assertThrows(ApiException.class,
                () -> authService.resetPassword(resetPasswordRequest));
        assertEquals(GeneralResponseCode.USER_NOT_FOUND, ex.getResponseCode());
    }

}
