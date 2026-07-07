package com.sparta.ordering.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 정보")
public record UserCreateRequest(

        @NotNull(message = "사용자 ID는 null이어서는 안 됩니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,10}$",
                message = "사용자 ID는 4자 이상 10자 이하의 알파벳 소문자와 숫자로만 구성되어야 합니다."
        )
        String userName,

        @NotBlank(message = "닉네임은 null이어서는 안 됩니다.")
        String nickName,

        @NotNull(message = "비밀번호는 null이어서는 안 됩니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8-15자 사이여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&\\-])[A-Za-z\\d@$!%*?&\\-]{8,15}$",
                message = "비밀번호는 8~15자의 알파벳 대소문자, 숫자, 특수문자가 각각 최소 1개 이상 포함되어야 합니다."
        )
        String password,

        @NotNull(message = "전화번호는 null이어서는 안됩니다.")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        String phoneNumber
) {

}
