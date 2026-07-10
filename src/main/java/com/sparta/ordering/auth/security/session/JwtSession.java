package com.sparta.ordering.auth.security.session;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p_jwt_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class JwtSession extends BaseUpdatableEntity {

    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID userId;

    @Column(columnDefinition = "varchar(512)", nullable = false, unique = false)
    private String accessToken;

    @Column(columnDefinition = "varchar(512)", nullable = false, unique = false)
    private String refreshToken;

    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant expirationTime;

    @Builder
    public JwtSession(UUID userId, String accessToken, String refreshToken, Instant expirationTime) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
    }


}
