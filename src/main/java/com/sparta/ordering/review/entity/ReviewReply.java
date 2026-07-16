package com.sparta.ordering.review.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
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
        name = "p_review_replies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_reply_review_author",
                        columnNames = {"review_id", "created_by", "unique_version"}
                )
        }
)
@Entity
public class ReviewReply extends BaseUpdatableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", columnDefinition = "uuid", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String replyText;

    @Column(name = "unique_version", nullable = false, columnDefinition = "uuid")
    private UUID uniqueVersion;

    @Builder
    public ReviewReply(Review review, String replyText) {
        this.review = review;
        this.replyText = replyText;
        uniqueVersion = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    public void update(String replyText) {
        if (replyText != null) {
            this.replyText = replyText;
        }
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);

        this.uniqueVersion = this.getId();
    }
}
