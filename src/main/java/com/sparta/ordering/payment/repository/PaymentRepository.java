package com.sparta.ordering.payment.repository;

import com.sparta.ordering.payment.entity.Payment;
import com.sparta.ordering.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndDeletedAtIsNull(UUID paymentId);

    Optional<Payment> findByIdAndOrder_Customer_IdAndDeletedAtIsNull(UUID paymentId, UUID userId);

    Optional<Payment> findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(UUID paymentId, UUID ownerId);

    Page<Payment> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Payment> findAllByOrder_Restaurant_User_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Page<Payment> findAllByOrder_Customer_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :newStatus WHERE p.id = :paymentId AND p.status = :currentStatus")
    int updateStatusByIdAndStatus(UUID paymentId, PaymentStatus currentStatus, PaymentStatus newStatus);

    @Query("""
           SELECT p FROM Payment p
           JOIN FETCH p.order
           WHERE p.id = :paymentId
           AND p.deletedAt IS NULL
           AND p.order.deletedAt IS NULL
           """)
    Optional<Payment> findByIdAndDeletedAtIsNullWithOrder(UUID paymentId);
}
