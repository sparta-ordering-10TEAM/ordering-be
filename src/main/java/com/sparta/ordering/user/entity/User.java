package com.sparta.ordering.user.entity;

import com.sparta.ordering.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false, unique = true)
    private String nickName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Role role;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder
    public User(String userName, String nickName, String email, String phoneNumber, Role role, String password, boolean locked,
                String profileImageUrl) {
        this.userName = userName;
        this.nickName = nickName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role == null ? Role.CUSTOMER : role;
        this.password = password;
        this.locked = locked;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String nickName, String phoneNumber, String profileImageUrl) {
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void updateLocked(boolean locked) {
        this.locked = locked;
    }

    public void updatePassword(String newPassword) {
        if (password != null && !password.isBlank()) {
            this.password = password;
        }
    }

    public void updateRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
    }
}
