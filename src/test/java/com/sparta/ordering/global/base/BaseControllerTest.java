package com.sparta.ordering.global.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetailService;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WithMockUser
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // SecurityConfig가 test 프로파일에서도 로드되므로 @Service 빈 목킹 필요
    @MockitoBean
    protected CustomUserDetailService customUserDetailService;

    // JwtAuthenticationFilter(@Component)의 의존성
    @MockitoBean
    protected JwtSessionService jwtSessionService;

    @MockitoBean
    protected UserRepository userRepository;
}