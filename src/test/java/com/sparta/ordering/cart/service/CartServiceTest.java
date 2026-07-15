package com.sparta.ordering.cart.service;

import com.sparta.ordering.cart.CartPolicy;
import com.sparta.ordering.cart.dto.CartItemQuantityRequest;
import com.sparta.ordering.cart.dto.CartItemRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Nested
    @DisplayName("내 카트 조회")
    class GetMyCart {
        @Test
        @DisplayName("성공 - 카트 존재")
        void test1() {

            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder().build();
            ReflectionTestUtils.setField(user, "id", userId);

            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);

            Cart cart = Cart.builder()
                    .user(user)
                    .restaurant(restaurant)
                    .build();
            UUID cartId = UUID.randomUUID();
            ReflectionTestUtils.setField(cart, "id", cartId);

            Product product = Product.builder()
                    .restaurant(restaurant)
                    .name("상품1")
                    .price(8000L)
                    .build();
            UUID productId = UUID.randomUUID();
            ReflectionTestUtils.setField(product, "id", productId);

            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(2)
                    .build();
            ReflectionTestUtils.setField(cartItem, "id", UUID.randomUUID());

            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId)).thenReturn(List.of(cartItem));

            // when
            CartResponse response = cartService.getMyCart(userId);

            // then
            assertThat(response.cartId()).isEqualTo(cartId);
            assertThat(response.restaurantId()).isEqualTo(restaurantId);
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).productId()).isEqualTo(productId);
            assertThat(response.items().get(0).quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공 - 카트 없음")
        void test2() {

            // given
            UUID userId = UUID.randomUUID();
            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            // when
            CartResponse response = cartService.getMyCart(userId);

            // then
            assertThat(response.cartId()).isNull();
            assertThat(response.restaurantId()).isNull();
            assertThat(response.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("장바구니 상품 추가")
    class AddItem {

        @Test
        @DisplayName("성공 - 카트  신규 상품 추가")
        void test1() {
            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder().build();
            ReflectionTestUtils.setField(user, "id", userId);

            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);

            UUID productId = UUID.randomUUID();
            Product product = Product.builder()
                    .restaurant(restaurant)
                    .name("상품1")
                    .price(8000L)
                    .build();
            ReflectionTestUtils.setField(product, "id", productId);

            UUID cartId = UUID.randomUUID();
            CartItemRequest request = new CartItemRequest(productId, 2);

            when(productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));
            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
                Cart cart = invocation.getArgument(0);
                ReflectionTestUtils.setField(cart, "id", cartId);
                return cart;
            });
            when(cartItemRepository.findByCart_IdAndProduct_IdAndDeletedAtIsNull(cartId, productId))
                    .thenReturn(Optional.empty());
            when(cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId))
                    .thenReturn(List.of());

            // when
            CartResponse response = cartService.addItem(userId, request);

            // then
            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(2);
            assertThat(captor.getValue().getProduct()).isEqualTo(product);
            assertThat(response.cartId()).isEqualTo(cartId);
            assertThat(response.restaurantId()).isEqualTo(restaurantId);
        }

        @Test
        @DisplayName("성공 - 이미 담긴 상품 수량 증가")
        void test2() {
            // given
            UUID userId = UUID.randomUUID();

            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", UUID.randomUUID());

            UUID cartId = UUID.randomUUID();
            Cart cart = Cart.builder().restaurant(restaurant).build();
            ReflectionTestUtils.setField(cart, "id", cartId);

            UUID productId = UUID.randomUUID();
            Product product = Product.builder()
                    .restaurant(restaurant)
                    .name("상품3")
                    .price(3000L)
                    .build();
            ReflectionTestUtils.setField(product, "id", productId);

            CartItem existingItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(3)
                    .build();
            UUID cartItemId = UUID.randomUUID();
            ReflectionTestUtils.setField(existingItem, "id", cartItemId);

            CartItemRequest request = new CartItemRequest(productId, 1);

            when(productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));
            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCart_IdAndProduct_IdAndDeletedAtIsNull(cartId, productId))
                    .thenReturn(Optional.of(existingItem));
            when(cartItemRepository.increaseQuantityAtomic(cartItemId, request.quantity(), CartPolicy.MAX_QUANTITY))
                    .thenReturn(1);
            when(cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId))
                    .thenReturn(List.of(existingItem));

            // when
            CartResponse response = cartService.addItem(userId, request);

            // then
            assertThat(response.items()).hasSize(1);
            verify(cartItemRepository).increaseQuantityAtomic(cartItemId, request.quantity(), CartPolicy.MAX_QUANTITY);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void test3() {
            // given
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            CartItemRequest request = new CartItemRequest(productId, 1);

            when(productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(productId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.addItem(userId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 다른 식당 상품")
        void test4() {
            // given
            UUID userId = UUID.randomUUID();

            Restaurant existingRestaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(existingRestaurant, "id", UUID.randomUUID());

            Restaurant otherRestaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(otherRestaurant, "id", UUID.randomUUID());

            Cart cart = Cart.builder().restaurant(existingRestaurant).build();
            ReflectionTestUtils.setField(cart, "id", UUID.randomUUID());

            UUID productId = UUID.randomUUID();
            Product product = Product.builder()
                    .restaurant(otherRestaurant)
                    .name("상품")
                    .price(1000L)
                    .build();
            ReflectionTestUtils.setField(product, "id", productId);

            CartItemRequest request = new CartItemRequest(productId, 1);

            when(productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));
            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(cart));

            // when & then
            assertThatThrownBy(() -> cartService.addItem(userId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.CART_DIFFERENT_RESTAURANT);
        }
        @Test
        @DisplayName("실패 - 수량이 99개 초과")
        void test5() {
            // given
            UUID userId = UUID.randomUUID();

            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", UUID.randomUUID());

            UUID cartId = UUID.randomUUID();
            Cart cart = Cart.builder().restaurant(restaurant).build();
            ReflectionTestUtils.setField(cart, "id", cartId);

            UUID productId = UUID.randomUUID();
            Product product = Product.builder()
                    .restaurant(restaurant)
                    .name("상품3")
                    .price(3000L)
                    .build();
            ReflectionTestUtils.setField(product, "id", productId);

            CartItem existingItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(3)
                    .build();
            ReflectionTestUtils.setField(existingItem, "id", UUID.randomUUID());

            CartItemRequest request = new CartItemRequest(productId, 97);

            when(productRepository.findByIdAndDeletedAtIsNullAndRestaurant_DeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));
            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCart_IdAndProduct_IdAndDeletedAtIsNull(cartId, productId))
                    .thenReturn(Optional.of(existingItem));

            // when & then
            assertThatThrownBy(() -> cartService.addItem(userId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.CART_ITEM_QUANTITY_INVALID);

        }

    }

    @Nested
    @DisplayName("카트 아이템 수량 업데이트")
    class UpdateQuantity{
        @Test
        @DisplayName("성공")
        void test1() {

            // given
            UUID userId = UUID.randomUUID();

            Cart cart = Cart.builder().build();
            UUID cartId = UUID.randomUUID();
            ReflectionTestUtils.setField(cart, "id", cartId);

            Product product = Product.builder().build();
            UUID productId = UUID.randomUUID();
            ReflectionTestUtils.setField(product, "id", productId);

            UUID cartItemId = UUID.randomUUID();
            CartItem cartItem = CartItem.builder()
                    .product(product)
                    .cart(cart)
                    .quantity(1)
                    .build();
            ReflectionTestUtils.setField(cartItem, "id", cartItemId);

            int quantity = 10;
            CartItemQuantityRequest request = new CartItemQuantityRequest(quantity);
            when(cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId))
                    .thenReturn(Optional.of(cartItem));

            when(cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId))
                    .thenReturn(List.of(cartItem));

            // when
            CartResponse response = cartService.updateItemQuantity(userId, cartItemId, request);

            // then
            assertThat(response.items().get(0).quantity()).isEqualTo(quantity);
            assertThat(response.cartId()).isEqualTo(cartId);
        }

        @Test
        @DisplayName("실패 - 수량 99 초과")
        void test2() {
            // given
            UUID userId = UUID.randomUUID();
            UUID cartItemId = UUID.randomUUID();
            CartItem cartItem = CartItem.builder().build();

            CartItemQuantityRequest request = new CartItemQuantityRequest(100);
            when(cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId))
                    .thenReturn(Optional.of(cartItem));

            // when && then
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, cartItemId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.CART_ITEM_QUANTITY_INVALID);
        }

        @Test
        @DisplayName("실패  존재하지 않는 카트 아이템")
        void test3() {
            // given
            UUID userId = UUID.randomUUID();
            UUID cartItemId = UUID.randomUUID();

            CartItemQuantityRequest request = new CartItemQuantityRequest(10);
            when(cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId))
                    .thenReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, cartItemId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.CART_ITEM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("장바구니 상품 삭제")
    class RemoveItem {

        @Test
        @DisplayName("성공 - 마지막 아이템 삭제 시 카트도 soft delete")
        void test1() {

            // given
            UUID userId = UUID.randomUUID();

            Restaurant restaurant = Restaurant.builder().build();
            UUID restaurantId = UUID.randomUUID();
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);

            UUID cartId = UUID.randomUUID();
            Cart cart = Cart.builder().restaurant(restaurant).build();
            ReflectionTestUtils.setField(cart, "id", cartId);

            UUID cartItemId = UUID.randomUUID();
            CartItem cartItem = CartItem.builder().cart(cart).build();
            ReflectionTestUtils.setField(cartItem, "id", cartItemId);


            when(cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId))
                    .thenReturn(Optional.of(cartItem));

            when(cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId))
                    .thenReturn(List.of());

            // when
            CartResponse response = cartService.removeItem(userId, cartItemId);

            // then
            assertThat(response.items()).isEmpty();
            assertThat(response.restaurantId()).isNull();
            assertThat(response.cartId()).isNull();
            assertThat(cart.getDeletedAt()).isNotNull();
            assertThat(cart.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 장바구니 아이템")
        void test3() {

            // given
            UUID userId = UUID.randomUUID();
            UUID cartItemId = UUID.randomUUID();

            when(cartItemRepository.findByIdAndCart_User_IdAndDeletedAtIsNullWithCart(cartItemId, userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.removeItem(userId, cartItemId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.CART_ITEM_NOT_FOUND);

        }
    }
    @Nested
    @DisplayName("장바구니 비우기")
    class ClearCart {

        @Test
        @DisplayName("성공")
        void test1() {

            // given
            UUID userId = UUID.randomUUID();

            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", UUID.randomUUID());

            UUID cartId = UUID.randomUUID();
            Cart cart = Cart.builder().restaurant(restaurant).build();
            ReflectionTestUtils.setField(cart, "id", cartId);

            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(cart));

            // when
            CartResponse response = cartService.clearCart(userId);

            // then
            verify(cartItemRepository).softDeleteAllByCartId(cartId, userId);
            assertThat(response.restaurantId()).isNull();
            assertThat(response.cartId()).isNull();
            assertThat(response.items()).isEmpty();
            assertThat(cart.getDeletedAt()).isNotNull();
            assertThat(cart.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 카트")
        void test2() {
            // given
            UUID userId = UUID.randomUUID();
            when(cartRepository.findByUser_IdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.clearCart(userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.CART_NOT_FOUND);
        }
    }
}