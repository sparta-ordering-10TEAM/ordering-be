package com.sparta.ordering.order.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.order.controller.api.OrderApi;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.dto.OrderDetailResponse;
import com.sparta.ordering.order.dto.OrderListResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
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
                                                                           @AuthenticationPrincipal UUID userId
                                                           ) {
        OrderCreateResponse response = orderService.create(request, userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/orders")
    public ResponseEntity<GeneralResponse<Page<OrderListResponse>>> getOrders(@AuthenticationPrincipal UUID userId,
                                                                              @PageableDefault(size = 10,
                                                                                      sort = "createdAt",
                                                                                      direction = Sort.Direction.DESC)Pageable pageable) {
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, orderService.getOrders(userId, pageable));
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<GeneralResponse<OrderDetailResponse>> getOrder(@AuthenticationPrincipal UUID userId,
                                                                         @PathVariable UUID orderId) {
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, orderService.getOrder(userId, orderId));
    }
}