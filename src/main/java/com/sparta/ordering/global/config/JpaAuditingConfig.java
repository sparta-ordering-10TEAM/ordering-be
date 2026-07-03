package com.sparta.ordering.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
/*
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal().equals("anonymousUser")) {
                // 인증 정보가 없을 때 (비회원 요청, 회원가입 등)
                return Optional.empty();
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return Optional.of(userDetails.getUserId());
        };
    }
 */
}