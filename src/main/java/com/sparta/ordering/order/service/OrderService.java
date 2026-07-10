package com.sparta.ordering.order.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.global.util.PageableUtils;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.dto.OrderDetailResponse;
import com.sparta.ordering.order.dto.OrderListResponse;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ORDER_NUMBER_LENGTH = 8;
    private static final int ORDER_NUMBER_RETRY_COUNT = 10;
    private static final int ORDER_SAVE_RETRY_COUNT = 3;
    private static final String ORDER_NUMBER_CONSTRAINT_KEY = "order_number";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderSaveService orderSaveService;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderCreateResponse create(OrderCreateRequest request, UUID userId) {

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (user.getRole() != Role.CUSTOMER) {
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

        return saveWithRetry(request, user, restaurant, productMap);
    }

    @Transactional(readOnly = true)
    public Page<OrderListResponse> getOrders(UUID userId, Pageable pageable) {
        PageableUtils.validateSort(pageable, Set.of("createdAt"));
        Pageable normalizedPageable = PageableUtils.normalizePageSize(pageable);

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        Page<Order> orders = switch (user.getRole()) {
            case CUSTOMER -> orderRepository.findAllByUserIdWithRestaurant(userId, normalizedPageable);
            case OWNER -> orderRepository.findAllByOwnerIdWithRestaurant(userId, normalizedPageable);
            case MANAGER, MASTER -> orderRepository.findAllWithRestaurant(normalizedPageable);
        };

        if (orders.isEmpty()) {
            return Page.empty(normalizedPageable);
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
            case CUSTOMER -> orderRepository.findByUserIdWithRestaurantAndOrderItems(userId, orderId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

            case OWNER -> orderRepository.findByOwnerIdWithRestaurantAndOrderItems(userId, orderId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

            case MANAGER, MASTER -> orderRepository.findByIdWithRestaurantAndOrderItems(orderId)
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));
        };

        return OrderDetailResponse.from(order);
    }

    private OrderCreateResponse saveWithRetry(OrderCreateRequest request,
                                                   User user,
                                                   Restaurant restaurant,
                                                   Map<UUID, Product> productMap) {
        for (int i=0; i<ORDER_SAVE_RETRY_COUNT; i++) {
            Order newOrder = createNewOrder(request, user, restaurant, productMap);

            try {
                orderSaveService.save(newOrder);
                return OrderCreateResponse.from(newOrder);
            } catch (DataIntegrityViolationException e) {
                if (isOrderNumberUniqueViolation(e)) {
                    continue;
                }
                throw e;
            }

        }

        throw new ApiException(GeneralResponseCode.ORDER_NUMBER_GENERATION_FAILED);
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

    private boolean isOrderNumberUniqueViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        return message != null && message.contains(ORDER_NUMBER_CONSTRAINT_KEY);
    }

    private String generateOrderNumber() {
        for (int i=0; i<ORDER_NUMBER_RETRY_COUNT; i++) {
            String orderNumber = createRandomOrderNumber();

            if (!orderRepository.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
        }

        throw new ApiException(GeneralResponseCode.ORDER_NUMBER_GENERATION_FAILED);
    }

    private String createRandomOrderNumber() {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<ORDER_NUMBER_LENGTH; i++) {
            int id = SECURE_RANDOM.nextInt(ORDER_NUMBER_CHARS.length());
            sb.append(ORDER_NUMBER_CHARS.charAt(id));
        }
        return sb.toString();
    }
}