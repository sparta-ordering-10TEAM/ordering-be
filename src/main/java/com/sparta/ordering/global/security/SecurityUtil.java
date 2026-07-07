package com.sparta.ordering.global.security;


import com.sparta.ordering.user.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityUtil {

    // 인증된 사용자가 주어진 role 중 하나라도 가지고 있는지 확인
    public static boolean hasAnyRole(Authentication authentication, Role... roles) {

        Set<String> requiredAuthorities = Arrays.stream(roles)
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredAuthorities::contains);


    }
}
