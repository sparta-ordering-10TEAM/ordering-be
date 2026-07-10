package com.sparta.ordering.order.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "request_message")
    private String requestMessage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(String orderNumber, Restaurant restaurant, User user, String deliveryAddress,
                  String requestMessage
    ) {
        this.orderNumber = orderNumber;
        this.restaurant = restaurant;
        this.user = user;
        this.totalPrice = 0L;
        this.deliveryAddress = deliveryAddress;
        this.requestMessage = requestMessage;
        this.orderStatus = OrderStatus.REQUESTED;
    }

    public static Order create(String orderNumber, Restaurant restaurant, User user, String deliveryAddress,
                               String requestMessage) {
        return new Order(orderNumber, restaurant, user, deliveryAddress, requestMessage);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
        this.totalPrice += orderItem.getTotalPrice();
    }
}