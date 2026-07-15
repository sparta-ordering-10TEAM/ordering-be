package com.sparta.ordering.order.entity;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_orders")
public class Order extends BaseUpdatableEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "request_message")
    private String requestMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(String orderNumber, Restaurant restaurant, User customer, String deliveryAddress,
                  String requestMessage
    ) {
        this.orderNumber = orderNumber;
        this.restaurant = restaurant;
        this.customer = customer;
        this.totalPrice = 0L;
        this.deliveryAddress = deliveryAddress;
        this.requestMessage = requestMessage;
        this.orderStatus = OrderStatus.REQUESTED;
    }

    public static Order create(String orderNumber, Restaurant restaurant, User customer, String deliveryAddress,
                               String requestMessage) {
        return new Order(orderNumber, restaurant, customer, deliveryAddress, requestMessage);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
        this.totalPrice += orderItem.getTotalPrice();
    }

    public void changeStatus(OrderStatus requestStatus) {
        validateStatus(requestStatus);
        this.orderStatus = requestStatus;
    }

    public void cancel(Instant now) {
        validateCancelStatus();
        validateCancelCheckTime(now);

        this.orderStatus = OrderStatus.CANCELLED;
    }

    private void validateStatus(OrderStatus requestStatus) {
        boolean valid = switch (this.orderStatus) {
            case REQUESTED ->
                requestStatus == OrderStatus.APPROVED
                        || requestStatus == OrderStatus.REJECTED;
            case APPROVED ->
                requestStatus == OrderStatus.COOKING_COMPLETED;
            case COOKING_COMPLETED ->
                requestStatus == OrderStatus.DELIVERING;
            case DELIVERING ->
                requestStatus == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };

        if (!valid) {
            throw new ApiException(GeneralResponseCode.ORDER_STATUS_TRANSITION_INVALID);
        }
    }

    private void validateCancelStatus() {
        boolean valid = this.orderStatus == OrderStatus.REQUESTED
                || this.orderStatus == OrderStatus.APPROVED;

        if (!valid) {
            throw new ApiException(GeneralResponseCode.ORDER_STATUS_TRANSITION_INVALID);
        }
    }

    private void validateCancelCheckTime(Instant now) {
        Instant deadline = getCreatedAt().plus(Duration.ofMinutes(5));

        if (now.isAfter(deadline)) {
            throw new ApiException(GeneralResponseCode.ORDER_CANCELLATION_TIME_EXPIRED);
        }
    }
}