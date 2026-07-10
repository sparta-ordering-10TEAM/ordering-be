package com.sparta.ordering.order.repository;

import com.sparta.ordering.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM Order o
            WHERE o.id = :id
            AND o.user.id = :userId
            AND o.deletedAt IS NULL
            """)
    Optional<Order> findByIdAndUserIdForUpdate(UUID id, UUID userId);

    @Query("""
           SELECT o FROM Order o
           JOIN FETCH o.restaurant
           WHERE o.user.id = :userId
           AND o.deletedAt IS NULL
           """)
    Page<Order> findAllByUserIdWithRestaurant(UUID userId, Pageable pageable);

    @Query("""
           SELECT o FROM Order o
           JOIN FETCH o.restaurant r
           WHERE r.user.id = :ownerId
           AND o.deletedAt IS NULL
           AND r.deletedAt IS NULL
           """
    )
    Page<Order> findAllByOwnerIdWithRestaurant(UUID ownerId, Pageable pageable);

    @Query("""
           SELECT o FROM Order o
           JOIN FETCH o.restaurant
           WHERE o.deletedAt IS NULL
           """
    )
    Page<Order> findAllWithRestaurant(Pageable pageable);

    @Query("""
           SELECT DISTINCT o FROM Order o
           JOIN FETCH o.restaurant
           LEFT JOIN FETCH o.orderItems oi
           WHERE o.id = :orderId
           AND o.user.id = :userId
           AND o.deletedAt IS NULL
           AND (oi IS NULL OR oi.deletedAt IS NULL)
           """)
    Optional<Order> findByUserIdWithRestaurantAndOrderItems(UUID userId, UUID orderId);

    @Query("""
           SELECT DISTINCT o FROM Order o
           JOIN FETCH o.restaurant r
           LEFT JOIN FETCH o.orderItems oi
           WHERE o.id = :orderId
           AND r.user.id = :ownerId
           AND o.deletedAt IS NULL
           AND r.deletedAt IS NULL
           AND (oi IS NULL OR oi.deletedAt IS NULL)
           """)
    Optional<Order> findByOwnerIdWithRestaurantAndOrderItems(UUID ownerId, UUID orderId);

    @Query("""
           SELECT DISTINCT o FROM Order o
           JOIN FETCH o.restaurant
           LEFT JOIN FETCH o.orderItems oi
           WHERE o.id = :orderId
           AND o.deletedAt IS NULL
           AND (oi IS NULL OR oi.deletedAt IS NULL)
           """)
    Optional<Order> findByIdWithRestaurantAndOrderItems(UUID orderId);
}