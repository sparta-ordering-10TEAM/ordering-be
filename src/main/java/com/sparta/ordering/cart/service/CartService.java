package com.sparta.ordering.cart.service;

import com.sparta.ordering.cart.CartPolicy;
import com.sparta.ordering.cart.dto.CartItemQuantityRequest;
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
import java.util.Optional;
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
        // 장바구니 없는 경우 빈 장바구니 반환
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

    // 기존 아이템이 있으면 수량 증가(원자적 UPDATE), 없으면 새로 생성
    private void addOrIncreaseCartItem(Cart cart, Product product, int quantity) {
        Optional<CartItem> existingItem = cartItemRepository.findByCart_IdAndProduct_IdAndDeletedAtIsNull(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            int updatedRow = cartItemRepository.increaseQuantityAtomic(existingItem.get().getId(), quantity, CartPolicy.MAX_QUANTITY);
            if (updatedRow == 0) {
                throw new ApiException(GeneralResponseCode.CART_ITEM_QUANTITY_INVALID);
            }
        } else {
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build());
        }
    }

    // 장바구니가 비어있으면 식당을 지정, 이미 있으면 같은 식당의 상품인지 검증
    private void validateSameRestaurant(Cart cart, Restaurant restaurant) {
        if (cart.getRestaurant() == null) {
            cart.changeRestaurant(restaurant);
        } else if (!cart.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ApiException(GeneralResponseCode.CART_DIFFERENT_RESTAURANT);
        }
    }

    // 유저의 장바구니를 조회하고, 없으면 새로 생성
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

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID cartItemId, CartItemQuantityRequest request) {
        CartItem cartItem = cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.CART_ITEM_NOT_FOUND));

        cartItem.changeQuantity(request.quantity());
        List<CartItem> cartItems = cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartItem.getCart().getId());
        return toCartResponse(cartItem.getCart(), cartItems);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID cartItemId) {

        // 카트 아이템 소유자 검증 + 삭제된 아이템 제외 (cart는 fetch join으로 함께 조회)
        CartItem cartItem = cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.CART_ITEM_NOT_FOUND));

        // soft delete 수행
        cartItem.softDelete(userId);

        // 아이템 삭제 후 카트가 비어있으면 Restaurant null 로 초기화
        Cart cart = cartItem.getCart();
        List<CartItem> itemList = cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cart.getId());
        if (itemList.isEmpty()) {
            cart.changeRestaurant(null);
        }

        return toCartResponse(cartItem.getCart(), itemList);
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {

        // 카트 소유자 검증
        Cart cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.CART_NOT_FOUND));

        // 장바구니 비우기 (벌크 연산)
        cartItemRepository.softDeleteAllByCartId(cart.getId(), userId);
        cart.changeRestaurant(null);

        return CartResponse.from(cart, List.of());
    }

    private CartResponse toCartResponse(Cart cart, List<CartItem> cartItems) {
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(CartItemResponse::from)
                .toList();

        return CartResponse.from(cart, itemResponses);
    }
}
