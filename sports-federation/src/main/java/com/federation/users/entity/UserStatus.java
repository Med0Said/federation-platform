package com.federation.users.entity;

/**
 * Lifecycle status of a user account — mapped to Postgres enum user_status.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION;

    public boolean isLoginAllowed() {
        return this == ACTIVE;
    }
}
