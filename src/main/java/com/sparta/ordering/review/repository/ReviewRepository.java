package com.sparta.ordering.review.repository;

import com.sparta.ordering.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByOrder_Restaurant_IdAndDeletedAtIsNull(UUID restaurantId, Pageable pageable);

    @Query(
            value = """
                    SELECT r FROM Review r
                    JOIN r.order o
                    JOIN OrderItem oi ON oi.order.id = o.id
                    WHERE oi.product.id = :productId
                    AND r.deletedAt IS NULL
                    """,
            countQuery = """
                     SELECT COUNT(r) FROM Review r
                     WHERE r.order.id IN (
                         SELECT oi.order.id FROM OrderItem oi
                         WHERE oi.product.id = :productId
                     )
                     AND r.deletedAt IS NULL
                    """
    )
    Page<Review> findByProductIdAndDeletedAtIsNull(UUID productId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r" +
            " WHERE r.order.restaurant.id = :restaurantId" +
            " AND r.deletedAt IS NULL")
    double calcRestaurantAverageRating(UUID restaurantId);

    Optional<Review> findByIdAndCustomer_IdAndDeletedAtIsNull(UUID id, UUID customerId);

    Optional<Review> findByOrder_IdAndCustomer_Id(UUID orderId, UUID customerId);
}
