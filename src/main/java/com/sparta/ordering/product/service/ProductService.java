package com.sparta.ordering.product.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.dto.ProductCreateRequestDto;
import com.sparta.ordering.product.dto.ProductResponseDto;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));

        return ProductResponseDto.from(product);
    }

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto request) {
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND));

        Product product = Product.builder()
                .restaurant(restaurant)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .build();

        return ProductResponseDto.from(productRepository.save(product));
    }
}
