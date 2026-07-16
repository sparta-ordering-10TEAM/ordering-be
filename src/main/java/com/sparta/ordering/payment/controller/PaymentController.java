package com.sparta.ordering.payment.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.global.security.SecurityUtil;
import com.sparta.ordering.payment.controller.api.PaymentApi;
import com.sparta.ordering.payment.dto.PaymentCancelRequest;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.dto.PaymentResponse;
import com.sparta.ordering.payment.facade.PaymentFacade;
import com.sparta.ordering.payment.service.PaymentService;
import com.sparta.ordering.user.entity.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Payment", description = "결제 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentController implements PaymentApi {

    private final PaymentFacade paymentFacade;
    private final PaymentService paymentService;

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/payments")
    public ResponseEntity<GeneralResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentFacade.processPayment(userDetails.getUserId(), request);
        return  GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<GeneralResponse<PaymentResponse>> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication
    ) {
        Role role = SecurityUtil.getRole(authentication);
        PaymentResponse response = paymentService.getPayment(paymentId, userDetails.getUserId(), role);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/payments")
    public ResponseEntity<GeneralResponse<Page<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Role role = SecurityUtil.getRole(authentication);
        Page<PaymentResponse> paymentResponse = paymentService.getPayments(userDetails.getUserId(), role, pageable);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, paymentResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<GeneralResponse<PaymentResponse>> cancelPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            @Valid @RequestBody PaymentCancelRequest request
    ) {
        Role role = SecurityUtil.getRole(authentication);
        PaymentResponse response = paymentFacade.processCancelPayment(paymentId, userDetails.getUserId(), role, request.reason());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
