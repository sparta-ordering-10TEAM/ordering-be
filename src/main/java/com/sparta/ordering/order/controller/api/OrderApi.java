package com.sparta.ordering.order.controller.api;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.dto.OrderDetailResponse;
import com.sparta.ordering.order.dto.OrderListResponse;
import com.sparta.ordering.order.dto.OrderStatusResponse;
import com.sparta.ordering.order.dto.OrderStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Order", description = "주문 관리 API")
@RequestMapping("/api")
public interface OrderApi {

    @Operation(
            summary = "주문 생성",
            description = "고객이 식당의 상품을 선택하여 주문을 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/orders")
    ResponseEntity<GeneralResponse<OrderCreateResponse>> createOrder(
            @RequestBody @Valid OrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "주문 목록 조회",
            description = "주문 목록을 페이징 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/orders")
    ResponseEntity<GeneralResponse<Page<OrderListResponse>>> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "주문 상세 조회",
            description = "주문 ID로 주문 상세 정보와 주문상품 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/orders/{orderId}")
    ResponseEntity<GeneralResponse<OrderDetailResponse>> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "주문 상태 변경",
            description = "가게 사장이 주문 처리 상태를 변경합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/orders/{orderId}/status")
    ResponseEntity<GeneralResponse<OrderStatusResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid OrderStatusUpdateRequest request
    );

    @Operation(
            summary = "주문 취소",
            description = "고객이 주문 생성 후 5분 이내에 주문을 취소합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/orders/{orderId}/cancel")
    ResponseEntity<GeneralResponse<OrderStatusResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "주문 삭제",
            description = "주문을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @DeleteMapping("/orders/{orderId}")
    ResponseEntity<GeneralResponse<Void>> deleteOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
