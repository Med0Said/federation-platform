package com.federation.common.util;

/**
 * Biological gender — mapped to the PostgreSQL enum {@code gender} defined in V1.
 * Shared across athletes, results and any other module that needs it.
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER
}
