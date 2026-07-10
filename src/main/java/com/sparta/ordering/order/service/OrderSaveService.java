package com.sparta.ordering.order.service;

import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderSaveService {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order save(Order order) {
        return orderRepository.saveAndFlush(order);
    }
}