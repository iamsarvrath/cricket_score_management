package com.cricket.livescore.model;

import java.time.Instant;
import java.util.List;

public record Match(
        long id,
        String title,
        String venue,
        String status,
        String battingTeam,
        String bowlingTeam,
        int runs,
        int wickets,
        int overs,
        int balls,
        int target,
        List<Player> players,
        List<String> recentBalls,
        int extras,
        int fours,
        int sixes,
        String striker,
        String nonStriker,
        String currentBowler,
        int maxOvers,
        List<Delivery> deliveries,
        int inningsNumber,
        int previousInningsRuns,
        Instant updatedAt
) {
    public String score() {
        return runs + "/" + wickets;
    }
}
