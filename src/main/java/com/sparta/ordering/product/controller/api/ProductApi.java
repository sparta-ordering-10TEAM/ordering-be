package com.sparta.ordering.product.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.dto.ErrorResponse;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.product.dto.ProductCreateRequest;
import com.sparta.ordering.product.dto.ProductResponse;
import com.sparta.ordering.product.dto.ProductSearchRequest;
import com.sparta.ordering.product.dto.ProductUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Product", description = "상품 관련 API")
@RequestMapping("/api")
public interface ProductApi {

    @Operation(summary = "상품 단건 조회", description = "productId로 상품을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 단건 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품 단건 조회 실패 (상품 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 상품을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            )
    })
    @GetMapping("/products/{productId}")
    ResponseEntity<GeneralResponse<ProductResponse>> getProductById(@PathVariable UUID productId);

    @Operation(summary = "상품 목록 조회", description = "가게의 상품을 조건(이름, 가격 범위)에 맞게 페이징 검색합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "상품 목록 조회 실패 (허용되지 않은 정렬 필드, 또는 요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "허용되지 않은 정렬 필드",
                                            value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": null}"
                                    ),
                                    @ExampleObject(
                                            name = "요청 값 검증 실패",
                                            value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"minPrice\": \"가격은 0보다 커야 합니다.\"}}"
                                    )
                            }
                    )
            )
    })
    @GetMapping("/restaurants/{restaurantId}/products")
    ResponseEntity<GeneralResponse<Page<ProductResponse>>> getProducts(
            @PathVariable UUID restaurantId,
            @Valid ProductSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "상품 생성", description = "가게에 새 상품을 등록합니다. MASTER/MANAGER는 임의의 가게에, OWNER는 본인 소유 가게에만 등록할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "상품 생성 성공",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "상품 생성 실패 (요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"price\": \"가격은 0보다 커야 합니다.\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "상품 생성 실패 (본인 소유 가게가 아님)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"인가되지 않은 요청입니다.\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품 생성 실패 (가게 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 가게를 찾을 수 없습니다.\", \"errors\": null}")
                    )
            )
    })
    @PostMapping("/products")
    ResponseEntity<GeneralResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication
    );

    @Operation(summary = "상품 수정", description = "상품을 수정합니다. MASTER/MANAGER는 임의의 상품을, OWNER는 본인 소유 가게의 상품만 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 수정 성공",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "상품 수정 실패 (요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"price\": \"가격은 0보다 커야 합니다.\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "상품 수정 실패 (본인 소유 가게가 아님)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"인가되지 않은 요청입니다.\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품 수정 실패 (상품 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 상품을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            )
    })
    @PatchMapping("/products/{productId}")
    ResponseEntity<GeneralResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication
    );

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다(논리 삭제). MASTER/MANAGER는 임의의 상품을, OWNER는 본인 소유 가게의 상품만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 삭제 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "상품 삭제 실패 (본인 소유 가게가 아님)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"인가되지 않은 요청입니다.\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품 삭제 실패 (상품 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 상품을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            )
    })
    @DeleteMapping("/products/{productId}")
    ResponseEntity<GeneralResponse<Void>> softDeleteProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication
    );
}
