package com.sparta.ordering.auth.service;

import com.sparta.ordering.auth.dto.ResetPasswordRequest;
import com.sparta.ordering.auth.security.session.JwtSessionRepository;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtSessionService jwtSessionService;
    private final JwtSessionRepository jwtSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;

    @Value("${resetpassword.validity-seconds}")
    private long PASSWORD_VALIDITY_SECONDS;

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        //TODO: 이메일 전송 로직 추가
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        //임시 비밀번호
        String tempPassword = generateTempPassword();
        String encodedTempPassword = passwordEncoder.encode(tempPassword);

        //DB업데이트
        user.setTempPassword(encodedTempPassword, Instant.now().plusSeconds(PASSWORD_VALIDITY_SECONDS));

        log.info("임시 비밀번호: {}", tempPassword);
    }

    //8자리 대소문자, 숫자, 특수문자가 각각 최소 1개 이상 포함
    private String generateTempPassword() {
        List<Character> charList = new ArrayList<>();
        charList.add(UPPER.charAt(secureRandom.nextInt(UPPER.length())));
        charList.add(LOWER.charAt(secureRandom.nextInt(LOWER.length())));
        charList.add(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
        charList.add(SPECIAL.charAt(secureRandom.nextInt(SPECIAL.length())));

        for (int i = 0; i < 4; i++) {
            charList.add(ALL_CHARS.charAt(secureRandom.nextInt(ALL_CHARS.length())));
        }

        Collections.shuffle(charList, secureRandom);
        StringBuilder sb = new StringBuilder();
        for (char ch : charList) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
