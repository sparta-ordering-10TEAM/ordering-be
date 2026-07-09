package com.sparta.ordering.cart.controller;

import com.sparta.ordering.cart.dto.CartResponse;
import com.sparta.ordering.cart.service.CartService;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart")
    public ResponseEntity<GeneralResponse<CartResponse>> getMyCart(
            @AuthenticationPrincipal UUID userId
    ) {
        CartResponse response = cartService.getMyCart(userId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
