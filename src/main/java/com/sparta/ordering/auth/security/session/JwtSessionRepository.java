package com.sparta.ordering.auth.security.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JwtSessionRepository extends JpaRepository<JwtSession, UUID> {
    Optional<JwtSession> findByRefreshToken(String refreshToken);
}
