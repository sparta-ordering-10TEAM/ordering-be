package com.sparta.ordering.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;


@Schema(description = "프로필 업데이트 정보")
public record ProfileUpdateRequest(
        String nickName,

        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        String phoneNumber
) {

}
