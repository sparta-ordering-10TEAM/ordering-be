package com.sparta.ordering.review.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_reviews")
@Entity
public class Review extends BaseUpdatableEntity {
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "order_id", nullable = false)
    //private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", columnDefinition = "uuid", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column
    private int rating;

    @Column
    private String comment;

    @Builder
    public Review(User user, int rating, String comment) {
        this.user = user;
        this.rating = rating;
        this.comment = comment;
    }
}
