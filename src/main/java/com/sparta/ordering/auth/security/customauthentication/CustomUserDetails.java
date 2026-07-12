package com.sparta.ordering.auth.security.customauthentication;

import com.sparta.ordering.user.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String userName;
    private final String password;
    private final Role role;
    private final boolean locked;
    private final Instant tempPasswordExpirationTime;

    public CustomUserDetails(UUID userId, String userName, String password, Role role, boolean locked, Instant tempPasswordExpirationTime) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.role = role;
        this.locked = locked;
        this.tempPasswordExpirationTime = tempPasswordExpirationTime;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + this.role.name()));
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (tempPasswordExpirationTime != null && tempPasswordExpirationTime.isBefore(Instant.now())) {
            log.info("만료된 임시 비밀번호");
            return false;
        }
        return true;
    }

    public UUID getUserId() {
        return this.userId;
    }
}
