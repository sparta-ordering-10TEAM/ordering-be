package com.sparta.ordering.user.dto.response;

import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse{
    private final UUID id;
    private final Instant createdAt;
    private final String userName;
    private final String nickName;
    private final Role role;

    public static UserResponse of(User user) {
        return new UserResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getUserName(),
                user.getNickName(),
                user.getRole()
        );
    }
}
