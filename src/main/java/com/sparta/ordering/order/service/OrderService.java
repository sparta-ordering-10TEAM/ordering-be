package com.sparta.ordering.order.service;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.dto.OrderDetailResponse;
import com.sparta.ordering.order.dto.OrderListResponse;
import com.sparta.ordering.order.dto.OrderStatusResponse;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
import com.sparta.ordering.order.entity.OrderStatus;
import com.sparta.ordering.order.repository.OrderItemRepository;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderCreateResponse create(OrderCreateRequest request, UUID userId) {

        User customer = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new ApiException(GeneralResponseCode.ORDER_ONLY_CUSTOMER);
        }

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getStatus() != RestaurantStatus.OPEN) {
            throw new ApiException(GeneralResponseCode.ORDER_RESTAURANT_NOT_OPEN);
        }

        List<UUID> productIds = request.orderItems().stream()
                .map(OrderCreateRequest.OrderItemRequest::productId)
                .toList();

        List<Product> products = productRepository.findAllByIdInAndDeletedAtIsNull(productIds);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        Order newOrder = createNewOrder(request, customer, restaurant, productMap);

        orderRepository.save(newOrder);

        return OrderCreateResponse.from(newOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderListResponse> getOrders(UUID userId, Pageable pageable) {

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        Page<Order> orders = switch (user.getRole()) {
            case CUSTOMER -> orderRepository.findAllByCustomerIdWithRestaurant(userId, pageable);
            case OWNER -> orderRepository.findAllByRestaurantOwnerIdWithRestaurant(userId, pageable);
            case MANAGER, MASTER -> orderRepository.findAllWithRestaurant(pageable);
        };

        if (orders.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> orderIds = orders.getContent().stream()
                .map(Order::getId)
                .toList();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_IdInAndDeletedAtIsNull(orderIds);

        Map<UUID, List<OrderItem>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));

        return orders.map(order ->
                OrderListResponse.from(order,
                        orderItemMap.getOrDefault(order.getId(), List.of())
                )
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(UUID userId, UUID orderId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        Order order = switch (user.getRole()) {
            case CUSTOMER -> orderRepository.findDetailByIdAndCustomerId(orderId, userId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

            case OWNER -> orderRepository.findDetailByIdAndRestaurantOwnerId(orderId, userId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

            case MANAGER, MASTER -> orderRepository.findDetailById(orderId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));
        };

        return OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderStatusResponse updateStatus(UUID orderId, UUID ownerId, OrderStatus requestStatus) {
        Order order = orderRepository.findByIdAndRestaurantOwnerIdForStatusUpdate(orderId, ownerId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));
        order.changeStatus(requestStatus);

        return OrderStatusResponse.from(order);
    }

    @Transactional
    public OrderStatusResponse cancelOrder(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByIdAndCustomerIdForCancel(orderId, customerId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));
        order.cancel(Instant.now());

        return OrderStatusResponse.from(order);
    }

    @Transactional
    public void deleteOrder(UUID orderId, UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        Order order = switch (user.getRole()) {
            case CUSTOMER -> orderRepository.findByIdAndCustomerIdForDelete(orderId, userId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

            case OWNER -> orderRepository.findByIdAndRestaurantOwnerIdForDelete(orderId, userId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

            case MANAGER, MASTER -> orderRepository.findByIdForDelete(orderId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));
        };

        order.softDelete(userId);
    }


    private Order createNewOrder(OrderCreateRequest request,
                                 User user,
                                 Restaurant restaurant,
                                 Map<UUID, Product> productMap
    ) {
        String orderNumber = generateOrderNumber();

        Order newOrder = Order.create(orderNumber,
                restaurant,
                user,
                request.deliveryAddress(),
                request.requestMessage()
        );

        for (OrderCreateRequest.OrderItemRequest orderItem : request.orderItems()) {
            Product product = productMap.get(orderItem.productId());

            if (product == null) {
                throw new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND);
            }

            if (!product.getRestaurant().getId().equals(restaurant.getId())) {
                throw new ApiException(GeneralResponseCode.ORDER_PRODUCT_RESTAURANT_MISMATCH);
            }

            OrderItem newOrderItem = OrderItem.create(product, orderItem.quantity());
            newOrder.addOrderItem(newOrderItem);
        }

        if (newOrder.getTotalPrice() < restaurant.getMinOrderAmount()) {
            throw new ApiException(GeneralResponseCode.ORDER_TOTAL_PRICE_INVALID);
        }

        return newOrder;
    }


    private String generateOrderNumber() {
        return TsidCreator.getTsid().toString();
    }
}
