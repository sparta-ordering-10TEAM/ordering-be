package com.sparta.ordering.review.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "p_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_order_user",
                        columnNames = {"order_id", "customer_id"}
                )
        }
)
@Entity
public class Review extends BaseUpdatableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", columnDefinition = "uuid", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", columnDefinition = "uuid", nullable = false)
    private User customer;

    @Column
    private int rating;

    @Column
    private String comment;

    @Builder
    public Review(Order order, User customer, int rating, String comment) {
        this.order = order;
        this.customer = customer;
        this.rating = rating;
        this.comment = comment;
    }

    public void updateReview(Integer rating, String comment) {
        if (rating != null) {
            this.rating = rating;
        }

        if (comment != null) {
            this.comment = comment;
        }
    }

    public boolean isDeleted() {
        return this.getDeletedAt() != null;
    }
}
