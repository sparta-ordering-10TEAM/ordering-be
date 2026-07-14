package com.sparta.ordering.auth.security.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p_jwt_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Getter
public class JwtSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", updatable = true)
    private Instant updatedAt;

    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID userId;

    @Column(columnDefinition = "text", nullable = false)
    private String accessToken;

    @Column(columnDefinition = "text")
    private String refreshToken;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant expirationTime;

    @Builder
    public JwtSession(UUID userId, String accessToken, String refreshToken, Instant expirationTime) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return this.expirationTime.isBefore(Instant.now());
    }

    public void update(String accessToken, String refreshToken, Instant expirationTime) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
    }
}
