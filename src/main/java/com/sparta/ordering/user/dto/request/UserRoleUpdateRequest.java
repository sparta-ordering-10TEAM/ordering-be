package com.sparta.ordering.user.dto.request;

import com.sparta.ordering.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "권한 수정 정보")
public record UserRoleUpdateRequest(
        Role role
) {

}
