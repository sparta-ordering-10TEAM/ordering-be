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
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public OrderCreateResponse create(OrderCreateRequest request, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));

        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException(GeneralResponseCode.ORDER_ONLY_CUSTOMER);
        }

        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ApiException(GeneralResponseCode.RESTAURANT_NOT_FOUND));

        String orderNumber = UUID.randomUUID().toString();

        Order newOrder = Order.create(orderNumber, restaurant, user, request.deliveryAddress(), request.requestMessage());

        List<UUID> productIds = request.orderItems().stream()
                .map(OrderCreateRequest.OrderItemRequest::productId)
                .toList();

        List<Product> products = productRepository.findAllById(productIds);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        for (OrderCreateRequest.OrderItemRequest orderItem : request.orderItems()) {
            Product product = productMap.get(orderItem.productId());

            if (product == null) {
                throw new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND);
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

}