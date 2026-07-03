package com.sparta.ordering.review.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_reviews")
@Entity
public class Review extends BaseUpdatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column
    private UUID id;

    @Builder
    public Review(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    //@Column
    //private Order orderId;

    //@Column
    //private User customerId;

    @Column
    private int rating;

    @Column
    private String comment;
}
