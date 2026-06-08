package com.federation.athletes.entity;

public enum AthleteCategory {
    YOUTH,
    JUNIOR,
    SENIOR,
    MASTERS,
    GRAND_MASTERS;

    /** Compute category from age. Used since the DB column is no longer generated. */
    public static AthleteCategory fromAge(int age) {
        if (age < 18) return YOUTH;
        if (age < 21) return JUNIOR;
        if (age < 35) return SENIOR;
        if (age < 50) return MASTERS;
        return GRAND_MASTERS;
    }
}
