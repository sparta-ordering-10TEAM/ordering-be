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

    public ProductResponseDto getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.INVALID_REQUEST, "존재 하지 않는 상품입니다."));

        return ProductResponseDto.from(product);
    }

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.INVALID_REQUEST, "존재하지 않는 가게입니다."));

        Product product = Product.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();

        return ProductResponseDto.from(productRepository.save(product));
    }
}
