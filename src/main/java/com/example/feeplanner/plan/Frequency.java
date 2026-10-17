package com.example.feeplanner.plan;

/** How often an instalment falls due. */
public enum Frequency {
    MONTHLY(1),
    QUARTERLY(3),
    HALF_YEARLY(6);

    private final int months;

    Frequency(int months) {
        this.months = months;
    }

    public int months() {
        return months;
    }
}
