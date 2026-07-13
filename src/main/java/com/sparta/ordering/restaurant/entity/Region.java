package com.sparta.ordering.restaurant.entity;

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

@Entity
@Table(
        name = "p_regions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_regions_parent_name",
                columnNames = {"parent_id", "name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseUpdatableEntity {

    public static final int MAX_DEPTH = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 1=시/도, 2=시/군/구, 3=동
     */
    @Column(nullable = false)
    private Integer depth;

    @Builder
    public Region(Region parent, String name) {
        this.parent = parent;
        this.name = name;
        this.depth = (parent == null) ? 1 : parent.getDepth() + 1;
    }

    public void rename(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public boolean isLeaf() {
        return depth == MAX_DEPTH;
    }
}
