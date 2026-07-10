package com.sparta.ordering.product.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.global.util.PageableUtils;
import com.sparta.ordering.product.dto.ProductCreateRequest;
import com.sparta.ordering.product.dto.ProductResponse;
import com.sparta.ordering.product.dto.ProductSearchRequest;
import com.sparta.ordering.product.dto.ProductUpdateRequest;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;

    private static final Set<Role> PRIVILEGED_ROLES = Set.of(Role.MANAGER, Role.MASTER);
    private static final Set<String> ALLOWED_SORT_FIELD = Set.of("name", "price", "createdAt");

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(productId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));

        return ProductResponse.from(product);
    }


    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(ProductSearchRequest request, UUID restaurantId, Pageable pageable) {
        PageableUtils.validateSort(pageable, ALLOWED_SORT_FIELD);
        Pageable normalizedPageable = PageableUtils.normalizePageSize(pageable);

        Page<Product> products = productRepository.searchProducts(restaurantId, request.name(), request.minPrice(), request.maxPrice(), normalizedPageable);

        return products.map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, UUID userId, Role role) {
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND));

        validateOwnerShip(userId, role, restaurant);

        Product product = Product.builder()
                .restaurant(restaurant)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductUpdateRequest request, UUID userId, Role role) {
        Product product = productRepository.findByIdAndDeletedAtIsNullWithRestaurant(productId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));

        validateOwnerShip(userId, role, product.getRestaurant());

        product.update(request.name(), request.description(), request.price());

        return ProductResponse.from(product);
    }


    @Transactional
    public void softDeleteProduct(UUID productId, UUID userId, Role role) {

        Product product = productRepository.findByIdAndDeletedAtIsNullWithRestaurant(productId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));

        validateOwnerShip(userId, role, product.getRestaurant());

        product.softDelete(userId);

    }

    private void validateOwnerShip(UUID userId, Role role, Restaurant restaurant) {
        boolean isPrivilege = PRIVILEGED_ROLES.contains(role);
        if (!isPrivilege && !restaurant.getUser().getId().equals(userId)) {
            throw new ApiException(AuthResponseCode.FORBIDDEN);
        }
    }
}
