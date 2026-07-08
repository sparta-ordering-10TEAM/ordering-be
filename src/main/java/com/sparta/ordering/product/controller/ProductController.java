package com.sparta.ordering.product.controller;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.global.security.SecurityUtil;
import com.sparta.ordering.product.dto.ProductCreateRequest;
import com.sparta.ordering.product.dto.ProductResponse;
import com.sparta.ordering.product.dto.ProductSearchRequest;
import com.sparta.ordering.product.dto.ProductUpdateRequest;
import com.sparta.ordering.product.service.ProductService;
import com.sparta.ordering.user.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @GetMapping("/restaurants/{restaurantId}/products")
    public ResponseEntity<GeneralResponse<Page<ProductResponse>>> getProducts(
            @PathVariable UUID restaurantId,
            ProductSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> responses = productService.getProducts(request, restaurantId, pageable);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, responses);
    }

    @Operation(summary = "상품 생성", description = "가게에 새 상품을 등록합니다.")
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER', 'OWNER')")
    public ResponseEntity<GeneralResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal UUID userId,
            Authentication authentication
    ) {

        Role role = SecurityUtil.getRole(authentication);
        ProductResponse responseDto = productService.createProduct(request, userId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, responseDto);
    }

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER', 'OWNER')")
    public ResponseEntity<GeneralResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal UUID userId,
            Authentication authentication
    ) {
        Role role = SecurityUtil.getRole(authentication);
        ProductResponse responseDto = productService.updateProduct(productId, request, userId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, responseDto);
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER', 'OWNER')")
    public ResponseEntity<GeneralResponse<Void>> softDeleteProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UUID userId,
            Authentication authentication
    ) {
        Role role = SecurityUtil.getRole(authentication);

        productService.softDeleteProduct(productId, userId, role);
        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

}
