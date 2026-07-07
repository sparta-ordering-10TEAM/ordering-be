package com.sparta.ordering.user.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserCreateRequest userCreateRequest) {
        // 중복 검사
        if (userRepository.existsByUserNameAndDeletedAtIsNull(userCreateRequest.userName())) {
            throw new ApiException(GeneralResponseCode.ALREADY_EXISTS_USER);
        }

        User user = userRepository.save(
                User.builder()
                        .userName(userCreateRequest.userName())
                        .nickName(userCreateRequest.nickName())
                        .phoneNumber(userCreateRequest.phoneNumber())
                        .password(passwordEncoder.encode(userCreateRequest.password()))
                        .locked(false)
                        .build()
        );

        return UserResponse.of(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse findProfile(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));
        return ProfileResponse.of(user);
    }


    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest profileUpdateRequest) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        user.updateProfile(profileUpdateRequest.nickName(), profileUpdateRequest.phoneNumber());
        return ProfileResponse.of(user);
    }
}
