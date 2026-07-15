package com.sparta.ordering.payment.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.dto.ErrorResponse;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.payment.dto.PaymentCancelRequest;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.dto.PaymentResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Payment", description = "결제 관련 API")
@RequestMapping("/api")
public interface PaymentApi {

    @Operation(summary = "결제 생성", description = "CUSTOMER 본인 주문에 대해 결제를 생성하고 PG 승인을 요청합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "결제 생성 실패 (결제 금액 불일치, 또는 요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "결제 금액 불일치",
                                            value = "{\"status\": 400, \"message\": \"결제 금액이 주문 금액과 일치하지 않습니다.\", \"errors\": null}"
                                    ),
                                    @ExampleObject(
                                            name = "요청 값 검증 실패",
                                            value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"amount\": \"결제 금액은 0보다 커야 합니다.\"}}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제 생성 실패 (주문 없음)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 주문을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "결제 생성 실패 (동시 요청으로 인한 중복 결제 시도)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 409, \"message\": \"이미 처리 중이거나 완료된 결제 요청입니다.\", \"errors\": null}")
                    )
            )
    })
    @PostMapping("/payments")
    ResponseEntity<GeneralResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentRequest request
    );

    @Operation(summary = "결제 단건 조회", description = "결제 ID로 결제 내역을 조회합니다. CUSTOMER/OWNER는 본인과 연관된 결제만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 조회 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제 조회 실패 (결제 없음, 또는 본인과 연관되지 않은 결제)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 결제 내역을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            )
    })
    @GetMapping("/payments/{paymentId}")
    ResponseEntity<GeneralResponse<PaymentResponse>> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication
    );

    @Operation(summary = "결제 목록 조회", description = "결제 목록을 페이징 조회합니다. CUSTOMER는 본인 주문 결제만, OWNER는 본인 가게 주문 결제만, MANAGER/MASTER는 전체를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "결제 목록 조회 실패 (허용되지 않은 정렬 필드)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": null}")
                    )
            )
    })
    @GetMapping("/payments")
    ResponseEntity<GeneralResponse<Page<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "결제 취소", description = "결제 완료 후 5분 이내에 결제를 취소합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 취소 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "결제 취소 실패 (취소 가능한 상태가 아님, 또는 요청 값 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "취소 가능한 상태 아님",
                                            value = "{\"status\": 400, \"message\": \"현재 상태에서는 요청을 처리할 수 없습니다\", \"errors\": null}"
                                    ),
                                    @ExampleObject(
                                            name = "요청 값 검증 실패",
                                            value = "{\"status\": 400, \"message\": \"유효하지 않은 요청입니다.\", \"errors\": {\"reason\": \"취소 사유는 필수입니다.\"}}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제 취소 실패 (결제 없음, 또는 본인과 연관되지 않은 결제)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"해당하는 결제 내역을 찾을 수 없습니다.\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "결제 취소 실패 (이미 취소 진행 중/처리됨, 또는 취소 가능 시간 초과)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "중복 취소 요청",
                                            value = "{\"status\": 409, \"message\": \"이미 취소가 진행 중이거나 처리된 결제입니다.\", \"errors\": null}"
                                    ),
                                    @ExampleObject(
                                            name = "취소 가능 시간 초과",
                                            value = "{\"status\": 409, \"message\": \"주문 생성 후 5분이 지나 취소할 수 없습니다.\", \"errors\": null}"
                                    )
                            }
                    )
            )
    })
    @PostMapping("/payments/{paymentId}/cancel")
    ResponseEntity<GeneralResponse<PaymentResponse>> cancelPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            @Valid @RequestBody PaymentCancelRequest request
    );
}
