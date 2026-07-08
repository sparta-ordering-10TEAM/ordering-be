package com.sparta.ordering.order.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.dto.OrderCreateRequest;
import com.sparta.ordering.order.dto.OrderCreateResponse;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ORDER_NUMBER_LENGTH = 8;
    private static final int ORDER_NUMBER_RETRY_COUNT = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

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

        String orderNumber = generateOrderNumber();

        Order newOrder = Order.create(orderNumber, restaurant, user, request.deliveryAddress(), request.requestMessage());

        List<UUID> productIds = request.orderItems().stream()
                .map(OrderCreateRequest.OrderItemRequest::productId)
                .toList();

        //  ProductRepository에 Soft Delete 제외 다건 조회 메서드가 추가되면 교체 -> findAllByIdInAndDeletedAtIsNull(productIds)
        List<Product> products = productRepository.findAllById(productIds);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

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

        orderRepository.save(newOrder);

        return OrderCreateResponse.from(newOrder);
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