package com.sparta.ordering.review.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_reviews")
@Entity
public class Review extends BaseUpdatableEntity {
    //@Column
    //private Order orderId;

    //@Column
    //private User customerId;

    @Column
    private int rating;

    @Column
    private String comment;

    @Builder
    public Review(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }
}
