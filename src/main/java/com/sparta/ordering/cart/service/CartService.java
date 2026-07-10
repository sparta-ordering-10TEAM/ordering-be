package com.sparta.ordering.cart.service;

import com.sparta.ordering.cart.dto.CartItemRequest;
import com.sparta.ordering.cart.dto.CartItemResponse;
import com.sparta.ordering.cart.dto.CartResponse;
import com.sparta.ordering.cart.entity.Cart;
import com.sparta.ordering.cart.entity.CartItem;
import com.sparta.ordering.cart.repository.CartItemRepository;
import com.sparta.ordering.cart.repository.CartRepository;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getMyCart(UUID userId) {
        return cartRepository.findByUser_Id(userId)
                .map(cart -> toCartResponse(cart, cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cart.getId())))
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(UUID userId, CartItemRequest request) {

        // 상품 검증
        Product product = productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(request.productId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));

        Restaurant restaurant = product.getRestaurant();

        // cart 존재하지 않으면 생성
        Cart cart = getOrCreateCart(userId, restaurant);

        // 다른 식당이 cart에 담겨 있는지 검증
        validateSameRestaurant(cart, restaurant);

        // 이미 cart에 존재 하는 item 이면 수량 증가 아니면 item 추가
        addOrIncreaseCartItem(cart, product, request.quantity());

        // cartResponse 반환
        List<CartItem> cartItems = cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cart.getId());
        return toCartResponse(cart, cartItems);
    }

    private void addOrIncreaseCartItem(Cart cart, Product product, int quantity) {
        cartItemRepository.findByCart_IdAndProduct_IdAndDeletedAtIsNull(cart.getId(), product.getId())
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity),
                        () -> cartItemRepository.save(CartItem.builder()
                                .cart(cart)
                                .product(product)
                                .quantity(quantity)
                                .build())
                );
    }

    private void validateSameRestaurant(Cart cart, Restaurant restaurant) {
        if (cart.getRestaurant() == null) {
            cart.changeRestaurant(restaurant);
        } else if (!cart.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ApiException(GeneralResponseCode.CART_DIFFERENT_RESTAURANT);
        }
    }

    private Cart getOrCreateCart(UUID userId, Restaurant restaurant) {
        return cartRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                            .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));
                    return cartRepository.save(
                            Cart.builder()
                                    .user(user)
                                    .restaurant(restaurant)
                                    .build()
                    );
                });
    }

    private CartResponse toCartResponse(Cart cart, List<CartItem> cartItems) {
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(CartItemResponse::from)
                .toList();

        return CartResponse.from(cart, itemResponses);
    }
}
