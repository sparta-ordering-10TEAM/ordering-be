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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공")
    void sign_up() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "testName", "Test123!", "010-1111-1111");

        given(passwordEncoder.encode("Test123!")).willReturn("encoded");
        when(userRepository.existsByUserNameAndDeletedAtIsNull(request.userName())).thenReturn(false);

        User user = User.builder()
                .userName(request.userName())
                .nickName(request.nickName())
                .password(request.password())
                .phoneNumber(request.phoneNumber())
                .locked(false)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        UserResponse result = userService.create(request);

        // then
        assertThat(result.getUserName()).isEqualTo("test");
        assertThat(result.getNickName()).isEqualTo("testName");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패")
    void validate_sign_up() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "testName", "Test123!", "010-1111-1111");

        when(userRepository.existsByUserNameAndDeletedAtIsNull(request.userName())).thenReturn(true);

        // when & then
        ApiException ex = assertThrows(ApiException.class, () -> userService.create(request));
        assertEquals(GeneralResponseCode.ALREADY_EXISTS_USER, ex.getResponseCode());
    }

    @Test
    @DisplayName("프로필 업데이트 성공")
    void update_profile() {
        //given
        UUID userId = UUID.randomUUID();

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "newName", "010-1111-1111"
        );

        User user = User.builder()
                .nickName("originalName")
                .phoneNumber("010-1234-1234")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        ProfileResponse result = userService.updateProfile(userId, request, null);

        // then
        assertThat(result.getNickName()).isEqualTo("newName");
        assertThat(result.getPhoneNumber()).isEqualTo("010-1111-1111");

    }

    @Test
    @DisplayName("프로필 업데이트 실패")
    void validate_profile_update() {
        // given
        UUID userId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                null, null
        );
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateProfile(userId,request,null));
        assertEquals(GeneralResponseCode.USER_NOT_FOUND, ex.getResponseCode());
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void find_Profile() {
        // given
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .nickName("test")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        ProfileResponse result = userService.findProfile(userId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNickName()).isEqualTo("test");
    }


}