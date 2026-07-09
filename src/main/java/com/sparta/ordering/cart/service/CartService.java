package com.sparta.ordering.cart.service;

import com.sparta.ordering.cart.dto.CartItemResponse;
import com.sparta.ordering.cart.dto.CartResponse;
import com.sparta.ordering.cart.entity.Cart;
import com.sparta.ordering.cart.entity.CartItem;
import com.sparta.ordering.cart.repository.CartItemRepository;
import com.sparta.ordering.cart.repository.CartRepository;
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

    @Transactional(readOnly = true)
    public CartResponse getMyCart(UUID userId) {

        // 카트 조회 하고 없으면 빈 카트 반환
        Cart cart = cartRepository.findByUser_Id(userId).orElse(null);
        if (cart == null) {
            return CartResponse.empty();
        }


        List<CartItem> cartItems = cartItemRepository.findByCart_IdAndDeletedAtIsNull(cart.getId());
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(CartItemResponse::from)
                .toList();
        return CartResponse.from(cart, itemResponses);
    }

}
