package com.sparta.ordering.user.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
}
