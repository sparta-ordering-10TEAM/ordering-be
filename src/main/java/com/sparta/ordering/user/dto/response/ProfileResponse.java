package com.sparta.ordering.user.dto.response;

import com.sparta.ordering.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProfileResponse {
    private final UUID userId;
    private final String userName;
    private final String nickName;
    private final String phoneNumber;
    private final String profileImageUrl;

    public static ProfileResponse of(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getUserName(),
                user.getNickName(),
                user.getPhoneNumber(),
                user.getProfileImageUrl()
        );
    }
}
