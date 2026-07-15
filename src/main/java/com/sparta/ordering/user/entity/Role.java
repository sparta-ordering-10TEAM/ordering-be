package com.sparta.ordering.user.entity;

public enum Role {
    CUSTOMER,
    OWNER,
    MANAGER,
    MASTER;

    public boolean isAdmin() {
        return this == MANAGER || this == MASTER;
    }
}
