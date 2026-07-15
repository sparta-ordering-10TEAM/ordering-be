package com.sparta.ordering.payment.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "p_payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_order",
                        columnNames = {"order_id", "unique_version"}
                )
        }
)
public class Payment extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "payment_key", unique = true)
    private String paymentKey;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "card_company", length = 50)
    private String cardCompany;

    @Column(name = "unique_version", nullable = false, columnDefinition = "uuid")
    private UUID uniqueVersion;

    @Builder
    public Payment(Order order, BigDecimal amount, String paymentKey) {
        this.order = order;
        this.amount = amount;
        this.paymentKey = paymentKey;
        this.paymentMethod = PaymentMethod.CARD;
        this.status = PaymentStatus.IN_PROGRESS;
        this.uniqueVersion = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    public void approve(Instant approvedAt, String cardCompany) {
        this.status = PaymentStatus.DONE;
        this.approvedAt = approvedAt;
        this.cardCompany = cardCompany;
    }

    public void fail(String reason) {
        this.status = PaymentStatus.ABORTED;
        this.failReason = reason;
        this.uniqueVersion = this.getId();
    }

    public void cancel(String reason, Instant canceledAt) {
        this.status = PaymentStatus.CANCELED;
        this.cancelReason = reason;
        this.canceledAt = canceledAt;
    }

    public void revertCancel() {
        this.status = PaymentStatus.DONE;
    }

}
