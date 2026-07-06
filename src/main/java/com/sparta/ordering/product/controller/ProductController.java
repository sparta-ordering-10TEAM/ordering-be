package com.sparta.ordering.product.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.product.dto.ProductResponseDto;
import com.sparta.ordering.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @GetMapping("/products/{productId}")
    public ResponseEntity<GeneralResponse<ProductResponseDto>> getProductById(@PathVariable UUID productId) {
        ProductResponseDto productResponse =  productService.getProduct(productId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, productResponse);
    }
}
