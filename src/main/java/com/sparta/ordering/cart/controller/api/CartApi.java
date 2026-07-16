package com.sparta.ordering.cart.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.cart.dto.CartItemQuantityRequest;
import com.sparta.ordering.cart.dto.CartItemRequest;
import com.sparta.ordering.cart.dto.CartResponse;
import com.sparta.ordering.global.dto.ErrorResponse;
import com.sparta.ordering.global.dto.GeneralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Cart", description = "장바구니 관련 API")
@RequestMapping("/api")
public interface CartApi {

    @Operation(
            summary = "내 장바구니 조회",
            description = "로그인한 CUSTOMER의 장바구니를 조회합니다. 장바구니가 없으면 빈 장바구니를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장바구니 조회 성공",
                    content = @Content(schema = @Schema(implementation = CartResponse.class)))
    })
    @GetMapping("/cart")
    ResponseEntity<GeneralResponse<CartResponse>> getMyCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "장바구니 상품 추가",
            description = "장바구니에 상품을 담습니다. 장바구니가 없으면 새로 생성하고, 이미 담긴 상품이면 수량을 증가시킵니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 추가 성공",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "상품 추가 실패 (다른 가게 상품/수량 초과, 또는 요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "다른 가게 상품",
                                            value = "{\"status\": 400, \"message\": \"장바구니에는 같은 식당의 상품만 담을 수 있습니다.\", \"errors\": null}"
                                    ),
                                    @ExampleObject(
                                            name = "요청 값 검증 실패",
                                            value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"quantity\": \"수량은 99개를 초과할 수 없습니다.\"}}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품 추가 실패 (상품 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 상품을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "상품 추가 실패 (동시 요청으로 인한 장바구니 생성 충돌)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 409, \"message\": \"장바구니를 생성하는 중 문제가 발생했습니다. 다시 시도해주세요.\", \"errors\": null}")
                    )
            )
    })
    @PostMapping("/cart/items")
    ResponseEntity<GeneralResponse<CartResponse>> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartItemRequest request
    );

    @Operation(
            summary = "장바구니 상품 수량 수정",
            description = "장바구니에 담긴 상품의 수량을 지정한 값으로 변경합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수량 수정 성공",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "수량 수정 실패 (요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"quantity\": \"수량은 99개를 초과할 수 없습니다.\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "수량 수정 실패 (장바구니 상품 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 장바구니 상품을 찾을 수 없습니다\", \"errors\": null}")
                    )
            )
    })
    @PatchMapping("/cart/items/{cartItemId}")
    ResponseEntity<GeneralResponse<CartResponse>> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody CartItemQuantityRequest request
    );

    @Operation(
            summary = "장바구니 상품 삭제",
            description = "장바구니에서 상품 1개를 삭제합니다. 마지막 상품을 삭제하면 장바구니도 함께 비워집니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 삭제 성공",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품 삭제 실패 (장바구니 상품 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 장바구니 상품을 찾을 수 없습니다\", \"errors\": null}")
                    )
            )
    })
    @DeleteMapping("/cart/items/{cartItemId}")
    ResponseEntity<GeneralResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID cartItemId
    );

    @Operation(
            summary = "장바구니 비우기",
            description = "장바구니에 담긴 상품을 전부 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장바구니 비우기 성공",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "장바구니 비우기 실패 (장바구니 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당 장바구니를 찾을 수 없습니다.\", \"errors\": null}")
                    )
            )
    })
    @DeleteMapping("/cart")
    ResponseEntity<GeneralResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
