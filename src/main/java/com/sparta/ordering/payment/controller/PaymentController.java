package com.sparta.ordering.payment.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.dto.PaymentResponse;
import com.sparta.ordering.payment.facade.PaymentFacade;
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
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/payments")
    public ResponseEntity<GeneralResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentFacade.processPayment(userId, request);
        return  GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<GeneralResponse<PaymentResponse>> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UUID userId
    ) {
        return null;
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/payments")
    public ResponseEntity<GeneralResponse<Page<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return null;
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<GeneralResponse<PaymentResponse>> cancelPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UUID userId
    ) {
        return null;
    }
}
