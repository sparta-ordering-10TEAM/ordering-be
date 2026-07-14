package com.sparta.ordering.order.service;

import com.github.f4b6a3.tsid.Tsid;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.dto.OrderStatusResponse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(productRepository.findAllByIdInAndDeletedAtIsNull(List.of(productId))).thenReturn(List.of(product));

            OrderCreateResponse response = orderService.create(request, userId);

            assertThat(response.status()).isEqualTo(OrderStatus.REQUESTED);
            assertThat(response.totalPrice()).isEqualTo(36000L);
            assertThat(Tsid.isValid(response.orderNumber())).isTrue();

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getCustomer()).isEqualTo(user);
            assertThat(savedOrder.getRestaurant()).isEqualTo(restaurant);
            assertThat(savedOrder.getTotalPrice()).isEqualTo(36000L);
            assertThat(savedOrder.getOrderItems()).hasSize(1);
            assertThat(Tsid.isValid(savedOrder.getOrderNumber())).isTrue();
            assertThat(response.orderNumber()).isEqualTo(savedOrder.getOrderNumber());

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(productRepository.findAllByIdInAndDeletedAtIsNull(List.of(productId))).thenReturn(List.of());

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(productRepository.findAllByIdInAndDeletedAtIsNull(List.of(productId))).thenReturn(List.of(product));

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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(productRepository.findAllByIdInAndDeletedAtIsNull(List.of(productId))).thenReturn(List.of(product));

            assertThatThrownBy(() -> orderService.create(request, userId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_TOTAL_PRICE_INVALID);

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("OWNER 주문 상태 변경")
    class UpdateOrderStatus {

        @ParameterizedTest
        @CsvSource({
                "REQUESTED, APPROVED",
                "REQUESTED, REJECTED",
                "APPROVED, COOKING_COMPLETED",
                "COOKING_COMPLETED, DELIVERING",
                "DELIVERING, COMPLETED"
        })
        @DisplayName("성공 - OWNER가 허용된 순서로 주문 상태를 변경한다")
        void successAllowedTransition(OrderStatus currentStatus, OrderStatus requestStatus) {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            Order order = createOrder(orderId, currentStatus, Instant.now());

            when(orderRepository.findByIdAndOwnerIdForUpdate(orderId, ownerId))
                    .thenReturn(Optional.of(order));

            // when
            OrderStatusResponse response = orderService.updateStatus(orderId, ownerId, requestStatus);

            // then
            assertThat(order.getOrderStatus()).isEqualTo(requestStatus);
            assertThat(response.status()).isEqualTo(requestStatus);
        }

        @Test
        @DisplayName("실패 - OWNER가 허용되지 않은 상태로 변경할 수 없다")
        void failInvalidTransition() {
            // given: 현재 상태와 허용되지 않은 요청 상태를 가진 실제 Order 준비
            UUID ownerId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            Order order = createOrder(orderId, OrderStatus.REQUESTED, Instant.now());

            when(orderRepository.findByIdAndOwnerIdForUpdate(orderId, ownerId))
                    .thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() ->
                    orderService.updateStatus(orderId, ownerId, OrderStatus.COMPLETED))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_STATUS_TRANSITION_INVALID);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
        }

        @Test
        @DisplayName("실패 - OWNER가 변경할 수 있는 주문을 찾을 수 없다")
        void failOrderNotFound() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            when(orderRepository.findByIdAndOwnerIdForUpdate(
                    orderId,
                    ownerId
            )).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    orderService.updateStatus(
                            orderId,
                            ownerId,
                            OrderStatus.APPROVED
                    )
            )
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("CUSTOMER 주문 취소")
    class CancelOrder {

        @ParameterizedTest
        @EnumSource(
                value = OrderStatus.class,
                names = {"REQUESTED", "APPROVED"}
        )
        @DisplayName("성공 - 고객이 취소 가능한 상태의 주문을 5분 이내에 취소한다")
        void successCancellableStatus(OrderStatus currentStatus) {
            // given
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            Order order = createOrder(
                    orderId,
                    currentStatus,
                    Instant.now().minus(Duration.ofMinutes(4))
            );

            when(orderRepository.findByIdAndCustomerIdForUpdate(
                    orderId,
                    customerId
            )).thenReturn(Optional.of(order));
            // when
            OrderStatusResponse response =
                    orderService.cancelOrder(
                            orderId,
                            customerId
                    );

            // then
            assertThat(order.getOrderStatus())
                    .isEqualTo(OrderStatus.CANCELLED);

            assertThat(response.orderId())
                    .isEqualTo(orderId);

            assertThat(response.status())
                    .isEqualTo(OrderStatus.CANCELLED);

            verify(orderRepository)
                    .findByIdAndCustomerIdForUpdate(
                            orderId,
                            customerId
                    );
        }

        @Test
        @DisplayName("실패 - 주문 생성 후 5분을 초과하면 취소할 수 없다")
        void failCancellationTimeExpired() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            Order order = createOrder(
                    orderId,
                    OrderStatus.REQUESTED,
                    Instant.now().minus(Duration.ofMinutes(6))
            );

            when(orderRepository.findByIdAndCustomerIdForUpdate(
                    orderId,
                    customerId
            )).thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() ->
                    orderService.cancelOrder(
                            orderId,
                            customerId
                    )
            )
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(
                            GeneralResponseCode.ORDER_CANCELLATION_TIME_EXPIRED
                    );

            assertThat(order.getOrderStatus())
                    .isEqualTo(OrderStatus.REQUESTED);
        }

        @Test
        @DisplayName("실패 - 취소 가능한 상태가 아니면 취소할 수 없다")
        void failInvalidCancellationStatus() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            Order order = createOrder(
                    orderId,
                    OrderStatus.COOKING_COMPLETED,
                    Instant.now().minus(Duration.ofMinutes(1))
            );

            when(orderRepository.findByIdAndCustomerIdForUpdate(
                    orderId,
                    customerId
            )).thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() ->
                    orderService.cancelOrder(
                            orderId,
                            customerId
                    )
            )
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(
                            GeneralResponseCode.ORDER_STATUS_TRANSITION_INVALID
                    );

            assertThat(order.getOrderStatus())
                    .isEqualTo(OrderStatus.COOKING_COMPLETED);
        }

        @Test
        @DisplayName("실패 - 고객이 취소할 수 있는 주문을 찾을 수 없다")
        void failOrderNotFound() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            when(orderRepository.findByIdAndCustomerIdForUpdate(
                    orderId,
                    customerId
            )).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    orderService.cancelOrder(
                            orderId,
                            customerId
                    )
            )
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_NOT_FOUND);
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

    private Order createOrder(UUID orderId, OrderStatus status, Instant createdAt) {
        Order order = Order.create(
                "ORDER01",
                createRestaurant(UUID.randomUUID(), RestaurantStatus.OPEN, 10000),
                createUser(UUID.randomUUID(), Role.CUSTOMER),
                "서울시 강남구",
                null
        );

        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "orderStatus", status);
        ReflectionTestUtils.setField(order, "createdAt", createdAt);

        return order;
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
