package com.sparta.ordering.global.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class SecurityRequestMatcher {

    // "/api"로 시작하지 않는 요청 -> 정적 리소스 허용
    private static final RequestMatcher NON_API = new NegatedRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/**"));

    private final RequestMatcher[] publicMatchers;

    public SecurityRequestMatcher() {
        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();

        RequestMatcher signUp        = path.matcher(HttpMethod.POST, "/api/users/sign-up");
        RequestMatcher signIn        = path.matcher(HttpMethod.POST, "/api/auth/sign-in");
        RequestMatcher signOut       = path.matcher("/api/auth/sign-out");
        RequestMatcher resetPassword = path.matcher(HttpMethod.POST, "/api/auth/reset-password");
        RequestMatcher refresh       = path.matcher(HttpMethod.POST, "/api/auth/refresh");

        this.publicMatchers = new RequestMatcher[]{ NON_API, signUp, signIn, signOut, resetPassword, refresh };
    }

    public RequestMatcher[] getPublicMatchers() {
        return publicMatchers;
    }
}