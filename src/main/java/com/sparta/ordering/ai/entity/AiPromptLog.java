package com.sparta.ordering.ai.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_ai_prompt_logs")
@Entity
public class AiPromptLog extends BaseUpdatableEntity {

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(name = "response_text", columnDefinition = "TEXT", nullable = false)
    private String responseText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "prompt_type", nullable = false)
    private PromptType promptType;

    @Builder
    public AiPromptLog(String prompt, String responseText, PromptType promptType) {
        this.prompt = prompt;
        this.responseText = responseText;
        this.promptType = promptType;
    }
}
