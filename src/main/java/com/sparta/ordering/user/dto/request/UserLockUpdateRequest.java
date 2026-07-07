package com.sparta.ordering.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계정 잠금 상태 변경 정보")
public record UserLockUpdateRequest(
        boolean locked
) {

}
