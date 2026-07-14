package com.sparta.ordering.user.init;

import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


/**
 * 사용자 관리 - 어드민 기능 서버 시작 시 어드민 계정 자동 초기화 email: system@ordering.io name: master password: Ordering1234!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name}")
    private String masterName;
    @Value("${app.admin.nickName}")
    private String masterNickName;
    @Value("${app.admin.email}")
    private String masterEmail;
    @Value("${app.admin.password}")
    private String masterPassword;
    @Value("${app.admin.phoneNumber}")
    private String masterPhoneNumber;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!userRepository.existsByEmailAndDeletedAtIsNull(masterEmail)
                && !userRepository.existsByUserNameAndDeletedAtIsNull(masterName)
                && !userRepository.existsByNickNameAndDeletedAtIsNull(masterNickName)) {
            userRepository.save(
                    User.builder()
                            .userName(masterName)
                            .nickName(masterNickName)
                            .email(masterEmail)
                            .phoneNumber(masterPhoneNumber)
                            .password(passwordEncoder.encode(masterPassword))
                            .role(Role.MASTER)
                            .build()
            );
            log.info("Master User Created: {}", masterEmail);
        }else {
            log.info("Master User({}) Already Exists", masterEmail);
        }
    }
}
