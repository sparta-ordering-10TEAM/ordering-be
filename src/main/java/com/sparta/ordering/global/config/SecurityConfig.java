package com.sparta.ordering.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.customauthentication.CustomAuthenticationFailureHandler;
import com.sparta.ordering.auth.security.customauthentication.CustomAuthenticationFilter;
import com.sparta.ordering.auth.security.customauthentication.CustomAuthenticationSuccessHandler;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetailService;
import com.sparta.ordering.auth.security.customauthentication.JwtLogoutHandler;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomAuthenticationFilter customAuthenticationFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   JwtLogoutHandler jwtLogoutHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll() //회원가입
                        // TODO: 일단은 모든 요청 허용
                        .anyRequest().permitAll()
                )
                .addFilterAt(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CustomAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .logoutSuccessUrl("/")//홈으로
                        .deleteCookies("refresh_token")//쿠키 삭제 - CustomAuthenticationSuccessHandler에서는 쿠키 이름을 "refresh_token"(언더스코어)으로 설정
                        .addLogoutHandler(jwtLogoutHandler) // JwtSession삭제 & 토큰 블랙리스트 추가 핸들러
                );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       CustomUserDetailService customUserDetailService) throws Exception{

        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        //  setUserDetailsService 대신 빌더의 userDetailsService()를 사용합니다.
        authenticationManagerBuilder
                .userDetailsService(customUserDetailService)
                .passwordEncoder(passwordEncoder());

        return authenticationManagerBuilder.build();
    }

    @Bean
    public CustomAuthenticationFilter customAuthenticationFilter(
            ObjectMapper objectMapper,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            AuthenticationFailureHandler authenticationFailureHandler,
            AuthenticationManager authenticationManager) {

        CustomAuthenticationFilter filter = new CustomAuthenticationFilter(objectMapper);
        // handler 설정
        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(authenticationFailureHandler);
        // /api/auth/sign-in 경로에 적용
        filter.setFilterProcessesUrl("/api/auth/sign-in");
        // authenticationManager 지정
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler(
            ObjectMapper objectMapper, JwtSessionService jwtSessionService) {
        return new CustomAuthenticationSuccessHandler(objectMapper, jwtSessionService);
    }

    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler(ObjectMapper objectMapper) {
        return new CustomAuthenticationFailureHandler(objectMapper);
    }

}
