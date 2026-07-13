package com.sparta.ordering.global.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.config.TestSecurityConfig;
import com.sparta.ordering.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@WithMockUser
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // JwtAuthenticationFilter(@Component Filter)의 의존성 — 없으면 컨텍스트 로드 실패
    @MockitoBean
    protected JwtSessionService jwtSessionService;

    @MockitoBean
    protected UserRepository userRepository;
}