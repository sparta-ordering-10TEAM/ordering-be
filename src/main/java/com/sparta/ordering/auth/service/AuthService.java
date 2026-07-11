package com.sparta.ordering.auth.service;

import com.sparta.ordering.auth.security.session.JwtSessionRepository;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtSessionService jwtSessionService;
    private final JwtSessionRepository jwtSessionRepository;
    private final UserRepository userRepository;

    public void resetPassword(String email) {

    }
}
