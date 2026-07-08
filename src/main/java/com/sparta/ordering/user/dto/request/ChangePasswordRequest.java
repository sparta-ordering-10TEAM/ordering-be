package com.sparta.ordering.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 변경 정보")
public record ChangePasswordRequest(
        @NotNull(message = "비밀번호는 null이어서는 안 됩니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8-15자 사이여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&\\-])[A-Za-z\\d@$!%*?&\\-]{8,15}$",
                message = "비밀번호는 8~15자의 알파벳 대소문자, 숫자, 특수문자가 각각 최소 1개 이상 포함되어야 합니다."
        )
        String password
) {

}
