package com.sparta.ordering.aipromptlog.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import com.sparta.ordering.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_ai_product_prompt_log")
@Entity
public class AiProductPromptLog extends BaseUpdatableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", columnDefinition = "uuid", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column
    private String prompt;

    @Column
    private String response;

    @Builder
    public AiProductPromptLog(Product product, String prompt, String response) {
        this.product = product;
        this.prompt = prompt;
        this.response = response;
    }
}
