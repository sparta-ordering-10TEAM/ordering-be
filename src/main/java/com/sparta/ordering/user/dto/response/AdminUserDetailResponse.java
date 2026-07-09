package com.sparta.ordering.user.dto.response;

import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminUserDetailResponse {
    private final UUID id;
    private final Instant createdAt;
    private final String userName;
    private final String nickName;
    private final String email;
    private final String phoneNumber;
    private final Role role;
    private final boolean locked;
    private final Instant deletedAt;
    private final UUID deletedBy;
    private final Instant updatedAt;

    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getUserName(),
                user.getNickName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isLocked(),
                user.getDeletedAt(),
                user.getDeletedBy(),
                user.getUpdatedAt()
        );
    }
}
