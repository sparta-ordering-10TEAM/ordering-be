package com.sparta.ordering.cart.controller;

import com.sparta.ordering.cart.dto.CartItemQuantityRequest;
import com.sparta.ordering.cart.dto.CartItemRequest;
import com.sparta.ordering.cart.dto.CartResponse;
import com.sparta.ordering.cart.service.CartService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> getMyCart(
            @AuthenticationPrincipal UUID userId
    ) {
        CartResponse response = cartService.getMyCart(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @PostMapping("/cart/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CartItemRequest request
    ) {
        CartResponse response = cartService.addItem(userId, request);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @PatchMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> updateItemQuantity(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody CartItemQuantityRequest request
    ) {
        CartResponse response = cartService.updateItemQuantity(userId, cartItemId, request);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @DeleteMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID cartItemId
    ) {
        CartResponse response = cartService.removeItem(userId, cartItemId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @DeleteMapping("/cart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GeneralResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal UUID userId
    ) {
        CartResponse response = cartService.clearCart(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }


}
