package com.federation.users.entity;

/**
 * Application roles — must match the PostgreSQL enum user_role and Spring Security
 * GrantedAuthority naming convention (ROLE_ prefix).
 */
public enum UserRole {

    ROLE_ADMIN,
    ROLE_FEDERATION_STAFF,
    ROLE_CLUB_MANAGER,
    ROLE_ATHLETE,
    ROLE_PUBLIC;

    /** Alias constants for use in @PreAuthorize expressions (avoids string literals). */
    public static final String ADMIN            = "ROLE_ADMIN";
    public static final String FEDERATION_STAFF = "ROLE_FEDERATION_STAFF";
    public static final String CLUB_MANAGER     = "ROLE_CLUB_MANAGER";
    public static final String ATHLETE          = "ROLE_ATHLETE";
    public static final String PUBLIC           = "ROLE_PUBLIC";

    public String displayName() {
        return name().replace("ROLE_", "").replace("_", " ");
    }
}
