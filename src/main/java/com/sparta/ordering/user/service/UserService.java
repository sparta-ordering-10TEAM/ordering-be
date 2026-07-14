package com.sparta.ordering.user.service;

import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.global.storage.FileStorageService;
import com.sparta.ordering.user.dto.request.ChangePasswordRequest;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtSessionService jwtSessionService;
    private final FileStorageService fileStorageService;

    @Cacheable(cacheNames = "users", key = "#userId")
    public Optional<User> findCachedById(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId);
    }

    @Transactional
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

    @Transactional
    public ProfileResponse updateProfile(UUID loginUserId, UUID userId, ProfileUpdateRequest profileUpdateRequest,
                                         MultipartFile profileImage) {
        validateOwnership(loginUserId, userId);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        String imageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            if (user.getProfileImageUrl() != null) {
                fileStorageService.delete(user.getProfileImageUrl());
            }
            imageUrl = fileStorageService.upload(profileImage, userId);
        }

        user.updateProfile(profileUpdateRequest.nickName(), profileUpdateRequest.phoneNumber(), imageUrl);
        return ProfileResponse.from(user);
    }

    @CacheEvict(cacheNames = "users", key = "#userId")
    @Transactional
    public void updatePassword(UUID loginUserId, UUID userId, ChangePasswordRequest changePasswordRequest) {
        validateOwnership(loginUserId, userId);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        String newPassword = passwordEncoder.encode(changePasswordRequest.password());
        user.updatePassword(newPassword);
        jwtSessionService.invalidateToken(userId);
    }

    @CacheEvict(cacheNames = "users", key = "#userId")
    @Transactional
    public void deactivate(UUID loginUserId, UUID userId) {
        validateOwnership(loginUserId, userId);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));
        user.softDelete(userId);
    }

    private void validateOwnership(UUID loginUserId, UUID targetUserId) {
        if (!loginUserId.equals(targetUserId)) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }
    }
}
