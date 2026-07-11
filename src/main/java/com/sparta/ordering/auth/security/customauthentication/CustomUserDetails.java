package com.sparta.ordering.auth.security.customauthentication;

import com.sparta.ordering.user.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String userName;
    private final String password;
    private final Role role;
    private final boolean locked;

    public CustomUserDetails(UUID userId, String userName, String password, Role role, boolean locked) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.role = role;
        this.locked = locked;
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

    public UUID getUserId() {
        return this.userId;
    }
}
