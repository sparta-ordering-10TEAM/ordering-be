package com.sparta.ordering.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sparta.ordering.auth.dto.ResetPasswordRequest;
import com.sparta.ordering.auth.dto.TokenRotationResult;
import com.sparta.ordering.auth.service.AuthService;
import com.sparta.ordering.global.base.BaseControllerTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AuthControllerTest extends BaseControllerTest {

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("쿠키의 리프레시 토큰으로 액세스 토큰을 조회한다.")
    void getMe_shouldReturnAccessToken() throws Exception {
        // given
        String refreshTokenValue = "refresh-token";
        String accessToken = "access-token";

        given(jwtSessionService.findAccessToken(refreshTokenValue)).willReturn(accessToken);

        // when & then
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("refresh_token", refreshTokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(accessToken));
    }

    @Test
    @DisplayName("리프레시 토큰과 액세스 토큰을 재발급한다.")
    void rotateToken_shouldReturnNewToken() throws Exception {
        // given
        String oldRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";
        String newAccessToken = "new-access-token";

        given(jwtSessionService.rotateToken(oldRefreshToken)).willReturn(
                new TokenRotationResult(newAccessToken, newRefreshToken));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(newAccessToken))
                .andExpect(cookie().value("refresh_token", newRefreshToken));
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 시 NoContent status을 반환한다.")
    void resetPassword() throws Exception {
        // given
        ResetPasswordRequest request = new ResetPasswordRequest("test@mail.com");
        doNothing().when(authService).resetPassword(request);

        // when & then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

}
