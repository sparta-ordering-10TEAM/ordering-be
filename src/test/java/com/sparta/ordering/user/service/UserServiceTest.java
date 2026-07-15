package com.sparta.ordering.user.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.global.storage.FileStorageService;
import com.sparta.ordering.user.dto.request.ChangePasswordRequest;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    private JwtSessionService jwtSessionService;

    @Mock
    private FileStorageService fileStorageService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("회원가입 성공")
    void sign_up() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "testName", "test@email.com", "Test123!", "010-1111-1111");

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
        UserCreateRequest request = new UserCreateRequest("test", "testName", "test@email.com", "Test123!", "010-1111-1111");

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
        ProfileResponse result = userService.updateProfile(userId, userId, request, null);

        // then
        assertThat(result.getNickName()).isEqualTo("newName");
        assertThat(result.getPhoneNumber()).isEqualTo("010-1111-1111");
    }

    @Test
    @DisplayName("프로필 업데이트 성공 - 이미지 포함")
    void update_profile_with_image() {
        // given
        UUID userId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest("newName", "010-1111-1111");
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );
        String expectedUrl = "http://localhost:8080/uploads/profiles/" + userId + "/some-uuid.jpg";

        User user = User.builder().nickName("originalName").phoneNumber("010-1234-1234").build();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(image, userId)).thenReturn(expectedUrl);

        // when
        ProfileResponse result = userService.updateProfile(userId, userId, request, image);

        // then
        assertThat(result.getProfileImageUrl()).isEqualTo(expectedUrl);
        verify(fileStorageService).upload(image, userId);
    }

    @Test
    @DisplayName("프로필 업데이트 성공 - 기존 이미지 교체")
    void update_profile_replaces_existing_image() {
        // given
        UUID userId = UUID.randomUUID();
        String oldUrl = "http://localhost:8080/uploads/profiles/" + userId + "/old.jpg";
        ProfileUpdateRequest request = new ProfileUpdateRequest(null, null);
        MockMultipartFile image = new MockMultipartFile(
                "image", "new.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );
        String newUrl = "http://localhost:8080/uploads/profiles/" + userId + "/new-uuid.jpg";

        User user = User.builder().nickName("name").phoneNumber("010-0000-0000").profileImageUrl(oldUrl).build();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(image, userId)).thenReturn(newUrl);

        // when
        userService.updateProfile(userId, userId, request, image);

        // then
        verify(fileStorageService).delete(oldUrl);
        verify(fileStorageService).upload(image, userId);
    }

    @Test
    @DisplayName("프로필 업데이트 실패 - 본인이 아닌 경우")
    void update_profile_forbidden() {
        // given
        UUID loginUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest("newName", "010-1111-1111");

        // when & then
        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateProfile(loginUserId, userId, request, null));
        assertEquals(AuthResponseCode.FORBIDDEN, ex.getResponseCode());
    }

    @Test
    @DisplayName("프로필 업데이트 실패 - 사용자 없음")
    void validate_profile_update() {
        // given
        UUID userId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest(null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateProfile(userId, userId, request, null));
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

    @Test
    @DisplayName("비밀번호 변경 성공")
    void update_password() {
        // given
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .nickName("test")
                .password(passwordEncoder.encode("originalPassword"))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        ChangePasswordRequest request = new ChangePasswordRequest("newPassword");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        userService.updatePassword(userId, userId, request);

        // then
        assertThat(passwordEncoder.matches("newPassword", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 본인이 아닌 경우")
    void update_password_forbidden() {
        // given
        UUID loginUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword");

        // when & then
        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updatePassword(loginUserId, userId, request));
        assertEquals(AuthResponseCode.FORBIDDEN, ex.getResponseCode());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 사용자 없음")
    void validate_update_password() {
        // given
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword");

        // when & then
        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updatePassword(userId, userId, request));
        assertEquals(GeneralResponseCode.USER_NOT_FOUND, ex.getResponseCode());
    }

    @Test
    @DisplayName("회원 논리 삭제 성공")
    void softDeleteUser() {
        UUID userId = UUID.randomUUID();
        User user = spy(User.builder()
                .nickName("test")
                .password(passwordEncoder.encode("originalPassword"))
                .build());
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));

        userService.deactivate(userId, userId);

        verify(user, times(1)).softDelete(userId);
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 본인이 아닌 경우")
    void deactivate_forbidden() {
        // given
        UUID loginUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when & then
        ApiException ex = assertThrows(ApiException.class,
                () -> userService.deactivate(loginUserId, userId));
        assertEquals(AuthResponseCode.FORBIDDEN, ex.getResponseCode());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 사용자 없음")
    void validate_soft_delete_user() {
        // given
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = assertThrows(ApiException.class, () -> userService.deactivate(userId, userId));
        assertEquals(GeneralResponseCode.USER_NOT_FOUND, ex.getResponseCode());
    }
}