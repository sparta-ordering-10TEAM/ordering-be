package com.sparta.ordering.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "비밀번호 초기화 요청 시 임시 비밀번호를 받는 이메일 주소 정보")
public record ResetPasswordRequest(
        @Email
        String email
) {

}
