package com.sparta.ordering.restaurant.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.dto.RestaurantCreateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.dto.RestaurantStatusUpdateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantUpdateRequest;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Restaurant", description = "음식점 관련 API")
@RequestMapping("/api")
public interface RestaurantApi {

    @Operation(summary = "음식점 목록 조회", description = "카테고리/지역/상태 조건으로 음식점 목록을 페이징 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음식점 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "음식점 목록 조회 실패 (유효하지 않은 요청)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @GetMapping("/restaurants")
    ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> getRestaurants(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) RestaurantStatus status,
            @PageableDefault Pageable pageable
    );

    @Operation(summary = "음식점 단건 조회", description = "restaurantId로 음식점을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음식점 단건 조회 성공",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "음식점 단건 조회 실패 (음식점 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @GetMapping("/restaurants/{restaurantId}")
    ResponseEntity<GeneralResponse<RestaurantResponse>> getRestaurant(@PathVariable UUID restaurantId);

    @Operation(
            summary = "내 음식점 목록 조회",
            description = "로그인한 OWNER의 음식점 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 음식점 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "내 음식점 목록 조회 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @GetMapping("/users/me/restaurants")
    ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> getOwnerRestaurants(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault Pageable pageable
    );

    @Operation(
            summary = "음식점 등록",
            description = "OWNER가 새 음식점을 등록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "음식점 등록 성공",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "음식점 등록 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "음식점 등록 실패 (사용자/카테고리 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PostMapping("/restaurants")
    ResponseEntity<GeneralResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody RestaurantCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "음식점 수정",
            description = "음식점 정보를 수정합니다. OWNER는 본인 가게만, MANAGER/MASTER는 모든 가게를 수정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음식점 수정 성공",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "음식점 수정 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "음식점 수정 실패 (음식점/카테고리 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping("/restaurants/{restaurantId}")
    ResponseEntity<GeneralResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "음식점 영업 상태 변경",
            description = "음식점 영업 상태를 변경합니다. OWNER는 본인 가게만, MANAGER/MASTER는 모든 가게를 변경할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음식점 영업 상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "음식점 영업 상태 변경 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "음식점 영업 상태 변경 실패 (음식점 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @PatchMapping("/restaurants/{restaurantId}/status")
    ResponseEntity<GeneralResponse<RestaurantResponse>> changeRestaurantStatus(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "음식점 삭제",
            description = "음식점을 논리 삭제합니다. OWNER는 본인 가게만, MANAGER/MASTER는 모든 가게를 삭제할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음식점 삭제 성공",
                    content = @Content(schema = @Schema())),
            @ApiResponse(
                    responseCode = "403",
                    description = "음식점 삭제 실패 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "음식점 삭제 실패 (음식점 없음)",
                    content = @Content(schema = @Schema(implementation = GeneralResponseCode.class))
            )
    })
    @DeleteMapping("/restaurants/{restaurantId}")
    ResponseEntity<GeneralResponse<Void>> deleteRestaurant(
            @PathVariable UUID restaurantId,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
