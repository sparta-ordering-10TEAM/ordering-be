package com.sparta.ordering.user.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.dto.request.ChangePasswordRequest;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserCreateRequest userCreateRequest) {
        // 중복 검사
        if (userRepository.existsByUserNameAndDeletedAtIsNull(userCreateRequest.userName()) ||
                userRepository.existsByEmailAndDeletedAtIsNull(userCreateRequest.email())) {
            throw new ApiException(GeneralResponseCode.ALREADY_EXISTS_USER);
        }

        if (userRepository.existsByNickNameAndDeletedAtIsNull(userCreateRequest.nickName())) {
            throw new ApiException(GeneralResponseCode.ALREADY_EXISTS_NICKNAME);
        }

        User user = userRepository.save(
                User.builder()
                        .userName(userCreateRequest.userName())
                        .nickName(userCreateRequest.nickName())
                        .email(userCreateRequest.email())
                        .phoneNumber(userCreateRequest.phoneNumber())
                        .password(passwordEncoder.encode(userCreateRequest.password()))
                        .locked(false)
                        .build()
        );

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse findProfile(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));
        return ProfileResponse.from(user);
    }


    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest profileUpdateRequest,
                                         MultipartFile profileImage) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        // TODO: 인프라 세팅 완료되면 ProfileImageUrl도 업데이트

        user.updateProfile(profileUpdateRequest.nickName(), profileUpdateRequest.phoneNumber(),null);
        return ProfileResponse.from(user);
    }

    public void updatePassword(UUID userId, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        String newPassword = passwordEncoder.encode(changePasswordRequest.password());
        user.updatePassword(newPassword);
    }

    public void deactivate(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));
        user.softDelete(userId);
    }
}
