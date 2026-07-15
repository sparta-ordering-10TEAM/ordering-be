package com.sparta.ordering.cart.controller;

import com.sparta.ordering.auth.security.customauthentication.CustomUserDetails;
import com.sparta.ordering.cart.controller.api.CartApi;
import com.sparta.ordering.cart.dto.CartItemQuantityRequest;
import com.sparta.ordering.cart.dto.CartItemRequest;
import com.sparta.ordering.cart.dto.CartResponse;
import com.sparta.ordering.cart.service.CartService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Cart", description = "장바구니 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartController implements CartApi {

    private final CartService cartService;

    @Override
    @GetMapping("/cart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> getMyCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse response = cartService.getMyCart(userDetails.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PostMapping("/cart/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartItemRequest request
    ) {
        CartResponse response = cartService.addItem(userDetails.getUserId(), request);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @PatchMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody CartItemQuantityRequest request
    ) {
        CartResponse response = cartService.updateItemQuantity(userDetails.getUserId(), cartItemId, request);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @DeleteMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID cartItemId
    ) {
        CartResponse response = cartService.removeItem(userDetails.getUserId(), cartItemId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @Override
    @DeleteMapping("/cart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse response = cartService.clearCart(userDetails.getUserId());
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
