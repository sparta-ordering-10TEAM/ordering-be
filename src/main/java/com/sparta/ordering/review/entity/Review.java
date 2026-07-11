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

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "p_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_order_customer",
                        columnNames = {"order_id", "customer_id", "unique_version"}
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

    @Column(name = "unique_version", nullable = false, columnDefinition = "uuid")
    private UUID uniqueVersion;
    // 기본 값을 가지는 필드를 Unique 제약조건에 포함해 중복 방지, 삭제 시 고유의 값으로 변경하여 중복체크로부터 분리
    // 임시 방편으로, 차후 DDL 쿼리 정의 시 함수형 인덱스를 사용해 개선할 수 있습니다

    @Builder
    public Review(Order order, User customer, int rating, String comment) {
        this.order = order;
        this.customer = customer;
        this.rating = rating;
        this.comment = comment;
        uniqueVersion = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    public void updateReview(Integer rating, String comment) {
        if (rating != null) {
            this.rating = rating;
        }

        if (comment != null) {
            this.comment = comment;
        }
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);

        this.uniqueVersion = this.getId();
    }
}
