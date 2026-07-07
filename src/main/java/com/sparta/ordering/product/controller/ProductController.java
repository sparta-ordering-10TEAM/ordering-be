package com.sparta.ordering.product.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.product.dto.ProductCreateRequest;
import com.sparta.ordering.product.dto.ProductResponse;
import com.sparta.ordering.product.dto.ProductUpdateRequest;
import com.sparta.ordering.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Product", description = "상품 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @Operation(summary = "상품 단건 조회", description = "productId로 상품을 조회합니다.")
    @GetMapping("/products/{productId}")
    public ResponseEntity<GeneralResponse<ProductResponse>> getProductById(@PathVariable UUID productId) {
        ProductResponse responseDto =  productService.getProduct(productId);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, responseDto);
    }

    @Operation(summary = "상품 생성", description = "가게에 새 상품을 등록합니다.")
    @PostMapping("/products")
    public ResponseEntity<GeneralResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        // TODO: 권한 체크
        ProductResponse responseDto = productService.createProduct(request);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, responseDto);
    }

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @PatchMapping("/products/{productId}")
    public ResponseEntity<GeneralResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId, @Valid @RequestBody ProductUpdateRequest request
    ) {
        // TODO: 권한 체크
        ProductResponse responseDto = productService.updateProduct(productId, request);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, responseDto);
    }

}
