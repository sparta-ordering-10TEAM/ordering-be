package com.sparta.ordering.payment.repository;

import com.sparta.ordering.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndDeletedAtIsNull(UUID paymentId);

    Optional<Payment> findByIdAndOrder_Customer_IdAndDeletedAtIsNull(UUID paymentId, UUID userId);

    Optional<Payment> findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(UUID paymentId, UUID ownerId);
}
