package com.sparta.ordering.user.service;

import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.dto.request.UserRoleUpdateRequest;
import com.sparta.ordering.user.dto.response.AdminUserDetailResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtSessionService jwtSessionService;

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

    @Test
    @DisplayName("회원 상세 조회 성공")
    void find_user_detail() {
        // given
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .userName("testUser")
                .nickName("testNick")
                .email("test@email.com")
                .phoneNumber("010-1234-5678")
                .role(Role.CUSTOMER)
                .locked(false)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        AdminUserDetailResponse result = adminService.findUserDetail(userId);

        // then
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUserName()).isEqualTo("testUser");
        assertThat(result.getEmail()).isEqualTo("test@email.com");
        assertThat(result.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(result.isLocked()).isFalse();
        assertThat(result.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("회원 상세 조회 실패 - 존재하지 않는 사용자")
    void find_user_detail_not_found() {
        // given
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = assertThrows(ApiException.class, () -> adminService.findUserDetail(userId));
        assertEquals(GeneralResponseCode.USER_NOT_FOUND, ex.getResponseCode());
    }

    @Test
    @DisplayName("회원 목록 조회 성공 - 조건 없이 전체 조회")
    void search_users_no_condition() {
        // given
        User user = User.builder()
                .userName("testUser")
                .nickName("testNick")
                .email("test@email.com")
                .phoneNumber("010-1234-5678")
                .role(Role.CUSTOMER)
                .locked(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);

        // when
        Page<AdminUserDetailResponse> result = adminService.searchUsers(null, null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("회원 목록 조회 성공 - userName 조건 조회")
    void search_users_by_username() {
        // given
        User user = User.builder()
                .userName("kimtest")
                .nickName("김테스트")
                .email("kim@email.com")
                .phoneNumber("010-1111-2222")
                .role(Role.CUSTOMER)
                .locked(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);

        // when
        Page<AdminUserDetailResponse> result = adminService.searchUsers("kim", null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("kimtest");
    }
}