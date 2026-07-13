package com.sparta.ordering.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank
        String userName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {

}
