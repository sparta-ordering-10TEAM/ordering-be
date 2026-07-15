package com.sparta.ordering.user.contorller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.base.BaseControllerTest;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.controller.UserController;
import com.sparta.ordering.user.dto.request.ChangePasswordRequest;
import com.sparta.ordering.user.dto.request.ProfileUpdateRequest;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class UserControllerTest extends BaseControllerTest {

    UUID testUserId;
    User testUser;
    UserResponse testUserResponse;
    ProfileResponse testProfileResponse;
    CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .userName("test")
                .password("Test1234!")
                .nickName("testNickName")
                .email("test@test.com")
                .phoneNumber("010-1234-5678")
                .locked(false)
                .build();
        ReflectionTestUtils.setField(testUser, "id", testUserId);

        testUserResponse = UserResponse.from(testUser);
        testProfileResponse = ProfileResponse.from(testUser);
        customUserDetails = new CustomUserDetails(testUserId, "test", "Test1234!", Role.CUSTOMER, false, null);

        // addFilters=false 환경에서 @AuthenticationPrincipal 주입을 위해 직접 SecurityContext 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회원 가입 성공")
    void signUp_success() throws Exception {
        // given
        UserCreateRequest userCreateRequest = new UserCreateRequest("test", "testNickName", "test@test.com",
                "Test1234!", "010-1234-5678");

        given(userService.create(userCreateRequest)).willReturn(testUserResponse);

        // when & then
        mockMvc.perform(
                        post("/api/users/sign-up")
                                .content(objectMapper.writeValueAsString(userCreateRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.userName").value("test"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 사용자")
    void 회원가입_실패() throws Exception {
        // given
        UserCreateRequest userCreateRequest = new UserCreateRequest("test", "testNickName", "test@test.com",
                "Test1234!", "010-1234-5678");

        given(userService.create(userCreateRequest))
                .willThrow(new ApiException(GeneralResponseCode.ALREADY_EXISTS_USER));

        // when & then
        mockMvc.perform(
                        post("/api/users/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userCreateRequest))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void 프로필_조회_성공() throws Exception {
        // given
        given(userService.findProfile(testUserId)).willReturn(testProfileResponse);

        // when & then
        mockMvc.perform(
                        get("/api/users/{userId}/profiles", testUserId)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.userName").value("test"));
    }

    @Test
    void 프로필_조회_실패() throws Exception {
        // given
        given(userService.findProfile(testUserId))
                .willThrow(new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(
                get("/api/users/{userId}/profiles", testUserId)
        ).andExpect(status().isNotFound());
    }

    @Test
    void 프로필_수정_성공() throws Exception {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest("newNickName", "010-9876-5432");

        given(userService.updateProfile(eq(testUserId), eq(testUserId), eq(request), any()))
                .willReturn(testProfileResponse);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}/profiles", testUserId)
                        .file(requestPart)
                        .with(rq -> {
                            rq.setMethod("PATCH");
                            return rq;
                        })
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.nickName").value("testNickName"));
    }

    @Test
    void 프로필_수정_실패() throws Exception {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest("newNickName", "010-9876-5432");

        given(userService.updateProfile(eq(testUserId), eq(testUserId), eq(request), any()))
                .willThrow(new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}/profiles", testUserId)
                        .file(requestPart)
                        .with(rq -> {
                            rq.setMethod("PATCH");
                            return rq;
                        })
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void 비밀번호_변경_성공() throws Exception {
        // given
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("NewPass1!");
        willDoNothing().given(userService).updatePassword(testUserId, testUserId, changePasswordRequest);

        // when & then
        mockMvc.perform(
                patch("/api/users/{userId}/password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest))
        ).andExpect(status().isOk());
    }

    @Test
    void 비밀번호_변경_validation_실패() throws Exception {
        // given - 빈 문자열은 @Size(min=8), @Pattern 검증 실패
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("");

        // when & then
        mockMvc.perform(
                patch("/api/users/{userId}/password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void 비밀번호_변경_사용자_검증_실패() throws Exception {
        // given
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("NewPass1!");
        willThrow(new ApiException(GeneralResponseCode.USER_NOT_FOUND)).given(userService)
                .updatePassword(testUserId, testUserId, changePasswordRequest);

        // when & then
        mockMvc.perform(
                patch("/api/users/{userId}/password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest))
        ).andExpect(status().isNotFound());
    }
}