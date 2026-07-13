package com.sparta.ordering.user.contorller;

import com.sparta.ordering.global.base.BaseControllerTest;
import com.sparta.ordering.user.controller.UserController;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.ProfileResponse;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class UserControllerTest extends BaseControllerTest {

    @MockitoBean
    private UserService userService;

    UUID testUserId;
    User testUser;
    UserResponse testUserResponse;
    ProfileResponse testProfileResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .userName("test")
                .password("Test1234!")
                .nickName("testNickName")
                .email("test@test.com")
                .locked(false)
                .build();
        ReflectionTestUtils.setField(testUser, "id", testUserId);

        testUserResponse = UserResponse.from(testUser);
        testProfileResponse = ProfileResponse.from(testUser);
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
}