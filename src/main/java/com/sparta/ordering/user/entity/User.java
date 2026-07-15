package com.sparta.ordering.user.entity;

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

import java.time.Instant;
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
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(nullable = true)
    private Instant tempPasswordExpirationTime;

    @Column(name = "approved_owner_by", nullable = true)
    private UUID approvedOwnerBy;

    @Builder
    public User(String userName, String nickName, String email, String phoneNumber, Role role, String password, boolean locked,
                String profileImageUrl, Instant tempPasswordExpirationTime) {
        this.userName = userName;
        this.nickName = nickName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role == null ? Role.CUSTOMER : role;
        this.password = password;
        this.locked = locked;
        this.profileImageUrl = profileImageUrl;
        this.tempPasswordExpirationTime = tempPasswordExpirationTime;
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

    public void updatePassword(String password) {
        if (password != null && !password.isBlank()) {
            this.password = password;
            this.tempPasswordExpirationTime = null;   // 정식으로 비밀번호 변경된 경우 다시 null로 설정
        }
    }

    public void updateRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }

    public void setTempPassword(String password, Instant tempPasswordExpirationTime) {
        if (password != null && !password.isBlank()) {
            this.password = password;
            this.tempPasswordExpirationTime = tempPasswordExpirationTime;
        }
    }

    public void approveAsOwner(UUID approvedBy) {
        this.role = Role.OWNER;
        this.approvedOwnerBy = approvedBy;
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
    }
}
