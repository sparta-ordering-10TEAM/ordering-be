package com.sparta.ordering.user.repository;

import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<User> withSearchCondition(
            String userName, Role role, Boolean locked) {
        return Specification
                .allOf(likeUserName(userName))
                .and(equalsRole(role))
                .and(equalsLocked(locked))
                .and(notDeleted());
    }

    private static Specification<User> likeUserName(String userName) {
        return (root, query, cb) -> userName == null ? null
                : cb.like(root.get("userName"), "%" + userName + "%");
    }

    private static Specification<User> equalsRole(Role role) {
        return (root, query, cb) -> role == null ? null
                : cb.equal(root.get("role"), role);
    }

    private static Specification<User> equalsLocked(Boolean locked) {
        return (root, query, cb) -> locked == null ? null
                : cb.equal(root.get("locked"), locked);
    }

    private static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
