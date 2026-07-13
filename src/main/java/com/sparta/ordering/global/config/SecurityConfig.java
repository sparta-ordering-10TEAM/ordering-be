package com.sparta.ordering.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.customauthentication.CustomAuthenticationFailureHandler;
import com.sparta.ordering.auth.security.customauthentication.CustomAuthenticationFilter;
import com.sparta.ordering.auth.security.customauthentication.CustomAuthenticationSuccessHandler;
import com.sparta.ordering.auth.security.customauthentication.CustomUserDetailService;
import com.sparta.ordering.auth.security.customauthentication.JwtLogoutHandler;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.global.security.JwtAuthenticationFilter;
import com.sparta.ordering.global.security.SecurityRequestMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomAuthenticationFilter customAuthenticationFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   JwtLogoutHandler jwtLogoutHandler,
                                                   SecurityRequestMatcher securityRequestMatcher) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityRequestMatcher.getPublicMatchers()).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/**")).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterAt(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CustomAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(logout -> logout
                        .logoutRequestMatcher(securityRequestMatcher.getSignOut())
                        .logoutSuccessUrl("/")
                        .deleteCookies("refresh_token")
                        .addLogoutHandler(jwtLogoutHandler)
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

        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(authenticationFailureHandler);

        filter.setFilterProcessesUrl("/api/auth/sign-in");

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

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_MASTER > ROLE_MANAGER > ROLE_OWNER\n"+
                "ROLE_MASTER > ROLE_MANAGER > ROLE_CUSTOMER");
    }

}
