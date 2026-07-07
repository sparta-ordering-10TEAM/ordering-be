package com.sparta.ordering.user.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.user.controller.api.UserApi;
import com.sparta.ordering.user.dto.request.UserCreateRequest;
import com.sparta.ordering.user.dto.response.UserResponse;
import com.sparta.ordering.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<GeneralResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest userCreateRequest) {
        UserResponse result = userService.create(userCreateRequest);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, result);
    }


}
