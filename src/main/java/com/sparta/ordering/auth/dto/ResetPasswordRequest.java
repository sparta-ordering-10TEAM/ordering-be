package com.sparta.ordering.auth.dto;

import jakarta.validation.constraints.Email;

public record ResetPasswordRequest(
        @Email
        String email
) {

}
