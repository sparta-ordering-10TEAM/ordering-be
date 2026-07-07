package com.sparta.ordering.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


@Schema(description = "프로필 업데이트 정보")
public record ProfileUpdateRequest(
        @NotNull(message = "닉네임은 null이어서는 안 됩니다.")
        String nickName,

        @NotNull(message = "전화번호는 null이어서는 안됩니다.")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        String phoneNumber
) {

}
