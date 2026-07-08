package com.sparta.ordering.order.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
import com.sparta.ordering.order.entity.OrderStatus;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("성공 - 주문과 주문상품을 함께 생성한다")
        void success() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            Restaurant restaurant = createRestaurant(restaurantId, RestaurantStatus.OPEN, 10000);
            Product product = createProduct(productId, restaurant, "치킨", 18000L);

            OrderCreateRequest request = createRequest(restaurantId, productId, 2);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));

            OrderCreateResponse response = orderService.create(request, userId);

            assertThat(response.status()).isEqualTo(OrderStatus.REQUESTED);
            assertThat(response.totalPrice()).isEqualTo(36000L);
            assertThat(response.orderNumber()).hasSize(8);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getUser()).isEqualTo(user);
            assertThat(savedOrder.getRestaurant()).isEqualTo(restaurant);
            assertThat(savedOrder.getTotalPrice()).isEqualTo(36000L);
            assertThat(savedOrder.getOrderItems()).hasSize(1);

            OrderItem savedOrderItem = savedOrder.getOrderItems().get(0);
            assertThat(savedOrderItem.getProduct()).isEqualTo(product);
            assertThat(savedOrderItem.getProductName()).isEqualTo("치킨");
            assertThat(savedOrderItem.getProductPrice()).isEqualTo(18000L);
            assertThat(savedOrderItem.getQuantity()).isEqualTo(2);
            assertThat(savedOrderItem.getTotalPrice()).isEqualTo(36000L);
            assertThat(savedOrderItem.getOrder()).isEqualTo(savedOrder);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void failUserNotFound() {
            UUID userId = UUID.randomUUID();
            OrderCreateRequest request = createRequest(UUID.randomUUID(), UUID.randomUUID(), 1);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.USER_NOT_FOUND);

            verifyNoInteractions(restaurantRepository, productRepository, orderRepository);
        }

        @Test
        @DisplayName("실패 - 고객 권한이 아닌 사용자")
        void failOnlyCustomer() {
            UUID userId = UUID.randomUUID();
            User user = createUser(userId, Role.OWNER);
            OrderCreateRequest request = createRequest(UUID.randomUUID(), UUID.randomUUID(), 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_ONLY_CUSTOMER);

            verifyNoInteractions(restaurantRepository, productRepository);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 식당")
        void failRestaurantNotFound() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            OrderCreateRequest request = createRequest(restaurantId, UUID.randomUUID(), 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.RESTAURANT_NOT_FOUND);

            verifyNoInteractions(productRepository);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패 - 영업 중이 아닌 식당")
        void failRestaurantNotOpen() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            Restaurant restaurant = createRestaurant(restaurantId, RestaurantStatus.CLOSED, 10000);
            OrderCreateRequest request = createRequest(restaurantId, UUID.randomUUID(), 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_RESTAURANT_NOT_OPEN);

            verifyNoInteractions(productRepository, orderRepository);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void failProductNotFound() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            Restaurant restaurant = createRestaurant(restaurantId, RestaurantStatus.OPEN, 10000);
            OrderCreateRequest request = createRequest(restaurantId, productId, 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of());

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PRODUCT_NOT_FOUND);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패 - 다른 식당의 상품")
        void failProductRestaurantMismatch() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();
            UUID otherRestaurantId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            Restaurant restaurant = createRestaurant(restaurantId, RestaurantStatus.OPEN, 10000);
            Restaurant otherRestaurant = createRestaurant(otherRestaurantId, RestaurantStatus.OPEN, 10000);
            Product product = createProduct(productId, otherRestaurant, "피자", 15000L);
            OrderCreateRequest request = createRequest(restaurantId, productId, 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_PRODUCT_RESTAURANT_MISMATCH);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패 - 최소 주문금액 미달")
        void failMinOrderAmount() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            Restaurant restaurant = createRestaurant(restaurantId, RestaurantStatus.OPEN, 20000);
            Product product = createProduct(productId, restaurant, "떡볶이", 10000L);
            OrderCreateRequest request = createRequest(restaurantId, productId, 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_TOTAL_PRICE_INVALID);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패 - 주문번호 생성 실패")
        void failOrderNumberGeneration() {
            UUID userId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();

            User user = createUser(userId, Role.CUSTOMER);
            Restaurant restaurant = createRestaurant(restaurantId, RestaurantStatus.OPEN, 10000);
            OrderCreateRequest request = createRequest(restaurantId, UUID.randomUUID(), 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(true);

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_NUMBER_GENERATION_FAILED);

            verify(orderRepository, times(10)).existsByOrderNumber(anyString());
            verifyNoInteractions(productRepository);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    private User createUser(UUID userId, Role role) {
        User user = User.builder()
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Restaurant createRestaurant(UUID restaurantId, RestaurantStatus status, Integer minOrderAmount) {
        Restaurant restaurant = Restaurant.builder()
                .status(status)
                .minOrderAmount(minOrderAmount)
                .build();
        ReflectionTestUtils.setField(restaurant, "id", restaurantId);
        return restaurant;
    }

    private Product createProduct(UUID productId, Restaurant restaurant, String name, Long price) {
        Product product = Product.builder()
                .restaurant(restaurant)
                .name(name)
                .price(price)
                .build();
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private OrderCreateRequest createRequest(UUID restaurantId, UUID productId, int quantity) {
        return new OrderCreateRequest(
                restaurantId,
                "서울시 강남구",
                null,
                List.of(new OrderCreateRequest.OrderItemRequest(productId, quantity))
        );
    }
}