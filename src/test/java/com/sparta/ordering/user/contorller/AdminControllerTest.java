package com.sparta.ordering.user.contorller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sparta.ordering.global.base.BaseControllerTest;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.controller.AdminController;
import com.sparta.ordering.user.dto.request.UserRoleUpdateRequest;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.service.AdminService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(roles = "MASTER")
public class AdminControllerTest extends BaseControllerTest {

    @MockitoBean
    private AdminService adminService;

    UUID testUserId;
    User testUser;
    UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .userName("testAdmin")
                .password("Test1234!")
                .nickName("testNickName")
                .email("admin@test.com")
                .phoneNumber("010-1234-5678")
                .role(Role.MASTER)
                .locked(false)
                .build();
        ReflectionTestUtils.setField(testUser, "id", testUserId);
        testUserResponse = UserResponse.from(testUser);
    }

    @Test
    void 잠금_상태_변경_성공() throws Exception {
        // given
        given(adminService.lock(testUserId)).willReturn(testUserId);

        // when & then
        mockMvc.perform(
                        patch("/api/admin/users/{userId}/lock", testUserId)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(testUserId.toString()));
    }

    @Test
    void 잠금_상태_변경_실패() throws Exception {
        // given
        given(adminService.lock(testUserId))
                .willThrow(new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(
                patch("/api/admin/users/{userId}/lock", testUserId)
        ).andExpect(status().isNotFound());
    }

    @Test
    void 권한_수정_성공() throws Exception {
        // given
        UserRoleUpdateRequest userRoleUpdateRequest = new UserRoleUpdateRequest(Role.OWNER);
        given(adminService.updateRole(testUserId, userRoleUpdateRequest)).willReturn(testUserResponse);

        // when & then
        mockMvc.perform(
                        patch("/api/admin/users/{userId}/role", testUserId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userRoleUpdateRequest))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(Role.MASTER.name()));
    }

    @Test
    void 권한_수정_실패() throws Exception {
        // given
        UserRoleUpdateRequest userRoleUpdateRequest = new UserRoleUpdateRequest(Role.OWNER);
        given(adminService.updateRole(testUserId, userRoleUpdateRequest))
                .willThrow(new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(
                patch("/api/admin/users/{userId}/role", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRoleUpdateRequest))
        ).andExpect(status().isNotFound());
    }
}