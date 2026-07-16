package com.sparta.ordering.order.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.order.controller.api.OrderApi;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.dto.OrderDetailResponse;
import com.sparta.ordering.order.dto.OrderListResponse;
import com.sparta.ordering.order.dto.OrderStatusResponse;
import com.sparta.ordering.order.dto.OrderStatusUpdateRequest;
import com.sparta.ordering.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/orders")
    public ResponseEntity<GeneralResponse<OrderCreateResponse>> createOrder(@Valid @RequestBody OrderCreateRequest request,
                                                                           @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OrderCreateResponse response = orderService.create(request, userDetails.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/orders")
    public ResponseEntity<GeneralResponse<Page<OrderListResponse>>> getOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                              @PageableDefault(size = 10,
                                                                                      sort = "createdAt",
                                                                                      direction = Sort.Direction.DESC) Pageable pageable) {
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, orderService.getOrders(userDetails.getUserId(), pageable));
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<GeneralResponse<OrderDetailResponse>> getOrder(@PathVariable UUID orderId,
                                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, orderService.getOrder(userDetails.getUserId(), orderId));
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<GeneralResponse<OrderStatusResponse>> updateOrderStatus(@PathVariable UUID orderId,
                                                                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                                  @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderStatusResponse response = orderService.updateStatus(orderId, userDetails.getUserId(), request.status());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<GeneralResponse<OrderStatusResponse>> cancelOrder(@PathVariable UUID orderId,
                                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderStatusResponse response = orderService.cancelOrder(orderId, userDetails.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<GeneralResponse<Void>> deleteOrder(@PathVariable UUID orderId,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.deleteOrder(orderId, userDetails.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

}