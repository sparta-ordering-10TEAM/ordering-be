package com.sparta.ordering.user.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.global.util.PageableUtils;
import com.sparta.ordering.user.dto.request.UserRoleUpdateRequest;
import com.sparta.ordering.user.dto.response.AdminUserDetailResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import com.sparta.ordering.user.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final UserRepository userRepository;


    public UUID lock(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (user.isLocked()) {
            throw new ApiException(GeneralResponseCode.USER_ALREADY_LOCKED);
        }

        user.updateLocked(true);

        // TODO: 잠긴 계정은 자동으로 로그아웃

        return user.getId();
    }

    public UUID unlock(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (!user.isLocked()) {
            throw new ApiException(GeneralResponseCode.USER_ALREADY_UNLOCKED);
        }

        user.updateLocked(false);

        return user.getId();
    }

    public UserResponse updateRole(UUID userId, UserRoleUpdateRequest userRoleUpdateRequest) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        //TODO: 권한 변경 시 자동으로 로그아웃

        user.updateRole(userRoleUpdateRequest.role());

        return UserResponse.from(user);
    }

    public AdminUserDetailResponse findUserDetail(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        return AdminUserDetailResponse.from(user);
    }

    public Page<AdminUserDetailResponse> searchUsers(String userName, Role role, Boolean locked, Pageable pageable) {
        Specification<User> spec = UserSpecification.withSearchCondition(userName, role, locked);
        return userRepository.findAll(spec, pageable)
                .map(AdminUserDetailResponse::from);
    }
}
