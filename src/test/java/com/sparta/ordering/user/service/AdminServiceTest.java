package com.sparta.ordering.user.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.dto.request.UserRoleUpdateRequest;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("잠금 상태 변경 성공")
    void update_lock() {
        // given
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .locked(false)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        adminService.lock(userId);

        // then
        assertThat(user.isLocked()).isEqualTo(true);
    }

    @Test
    @DisplayName("권한 수정 성공")
    void update_role() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.MANAGER);

        User user = User.builder()
                .locked(false)
                .role(Role.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        UserResponse result = adminService.updateRole(userId, request);

        // then
        assertThat(user.getRole()).isEqualTo(Role.MANAGER);
        assertThat(result.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    @DisplayName("권한 수정 실패")
    void validate_update_role() {
        // given
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.MANAGER);

        // when & then
        ApiException ex = assertThrows(ApiException.class, () -> adminService.updateRole(userId, request));
        assertEquals(GeneralResponseCode.USER_NOT_FOUND, ex.getResponseCode());
    }
}