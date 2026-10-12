package com.federation.registrations.entity;

import java.util.EnumSet;
import java.util.Set;

/** Mirrors the PostgreSQL enum {@code registration_status} (V4__enums.sql). */
public enum RegistrationStatus {
    PENDING,
    CONFIRMED,
    WAITLISTED,
    CANCELLED,
    DISQUALIFIED;

    /** Statuses that occupy a start slot and count toward capacity limits. */
    public static final Set<RegistrationStatus> ACTIVE = EnumSet.of(PENDING, CONFIRMED);

    public boolean isActive()   { return ACTIVE.contains(this); }
    public boolean isTerminal() { return this == CANCELLED || this == DISQUALIFIED; }
}
