package com.sparta.ordering.global.security;


import com.sparta.ordering.user.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class SecurityUtil {

    // 인증된 사용자의 role을 추출
    public static Role getRole(Authentication authentication) {
        String authority = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new IllegalStateException("인증 정보에 권한이 존재하지 않습니다."));


        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
