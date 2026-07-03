package com.sparta.ordering.review.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "t_reviews")
@Entity
public class Review extends BaseUpdatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column
    private UUID id;

    //@Column
    //private Order orderId;

    //@Column
    //private User customerId;

    @Column
    private int rating;

    @Column
    private String comment;
}
