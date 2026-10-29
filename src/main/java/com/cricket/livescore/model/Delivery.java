package com.cricket.livescore.model;

import java.time.Instant;

public record Delivery(
        int over,
        int ball,
        String striker,
        String bowler,
        int batRuns,
        int extras,
        String extraType,
        boolean wicket,
        String commentary,
        Instant recordedAt
) {
    public String label() {
        if (wicket) return "W";
        if ("wide".equals(extraType)) return extras + "Wd";
        if ("no-ball".equals(extraType)) return extras + "Nb";
        if ("bye".equals(extraType)) return extras + "B";
        if ("leg-bye".equals(extraType)) return extras + "Lb";
        return String.valueOf(batRuns);
    }
}
