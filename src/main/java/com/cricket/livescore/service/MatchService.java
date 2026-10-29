package com.cricket.livescore.service;

import com.cricket.livescore.model.Delivery;
import com.cricket.livescore.model.Match;
import com.cricket.livescore.model.Player;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class MatchService {
    private final Deque<Match> history = new ArrayDeque<>();
    private Match current = freshMatch("India", "Australia", "Wankhede Stadium, Mumbai", 20, 200);

    public synchronized Match getCurrentMatch() {
        return current;
    }

    public synchronized Match startMatch(String battingTeam, String bowlingTeam, String venue,
                                          int maxOvers, int target) {
        if (battingTeam.isBlank() || bowlingTeam.isBlank()
                || battingTeam.equalsIgnoreCase(bowlingTeam)) {
            throw new IllegalArgumentException("Enter two different team names");
        }
        if (maxOvers < 1 || maxOvers > 50 || target < 1) {
            throw new IllegalArgumentException("Overs must be 1-50 and target must be positive");
        }
        history.clear();
        current = freshMatch(battingTeam.trim(), bowlingTeam.trim(), venue.trim(), maxOvers, target);
        return current;
    }

    public synchronized Match recordDelivery(int batRuns, String extraType, int extraRuns,
                                              boolean wicket, String striker, String bowler) {
        if (!"LIVE".equals(current.status())) {
            throw new IllegalStateException("The innings is already " + current.status().toLowerCase());
        }
        if (wicket && current.wickets() >= 10) {
            throw new IllegalStateException("An innings cannot have more than 10 wickets");
        }
        if (batRuns < 0 || batRuns > 6 || extraRuns < 0 || extraRuns > 6) {
            throw new IllegalArgumentException("Runs must be between 0 and 6");
        }
        Match match = current;
        String actualStriker = validPlayer(striker, match) ? striker : match.striker();
        String actualBowler = bowler == null || bowler.isBlank() ? match.currentBowler() : bowler;
        boolean legalBall = extraType == null || extraType.isBlank()
                || "bye".equals(extraType) || "leg-bye".equals(extraType);
        int nextBallCount = match.overs() * 6 + match.balls() + (legalBall ? 1 : 0);
        int nextOvers = nextBallCount / 6;
        int nextBalls = nextBallCount % 6;
        int total = match.runs() + batRuns + extraRuns;
        int wickets = match.wickets() + (wicket ? 1 : 0);
        List<Player> players = updatePlayers(match.players(), actualStriker, batRuns, extraType,
                extraRuns, wicket);
        String label = wicket ? "W" : extraType == null || extraType.isBlank()
                ? String.valueOf(batRuns) : extraType;
        String commentary = commentary(actualStriker, actualBowler, batRuns, extraType, extraRuns, wicket);
        Delivery delivery = new Delivery(match.overs() + 1, match.balls() + 1, actualStriker,
                actualBowler, batRuns, extraRuns, extraType, wicket, commentary, Instant.now());
        List<Delivery> deliveries = new ArrayList<>(match.deliveries());
        deliveries.add(delivery);
        List<String> recent = new ArrayList<>(match.recentBalls());
        recent.add(label);
        if (recent.size() > 6) recent.remove(0);
        history.push(match);
        String nextStriker = wicket ? nextBatter(match.players(), actualStriker, match.nonStriker()) : actualStriker;
        String nextNonStriker = match.nonStriker();
        if (legalBall && (batRuns + extraRuns) % 2 == 1) {
            String switched = nextStriker;
            nextStriker = nextNonStriker;
            nextNonStriker = switched;
        }
        if (legalBall && nextBalls == 0) {
            String endOverStriker = nextStriker;
            nextStriker = nextNonStriker;
            nextNonStriker = endOverStriker;
        }
        String inningsStatus = match.inningsNumber() == 1
                ? firstInningsStatus(wickets, nextOvers, match.maxOvers())
                : status(total, match.target(), wickets, nextOvers, match.maxOvers());
        Match updated = new Match(match.id(), match.title(), match.venue(), inningsStatus,
                match.battingTeam(), match.bowlingTeam(), total, wickets, nextOvers, nextBalls,
                match.target(), players, recent, match.extras() + extraRuns,
                match.fours() + (batRuns == 4 ? 1 : 0), match.sixes() + (batRuns == 6 ? 1 : 0),
                nextStriker, nextNonStriker, actualBowler, match.maxOvers(), deliveries,
                match.inningsNumber(), match.previousInningsRuns(), Instant.now());
        if (match.inningsNumber() == 1 && !"LIVE".equals(inningsStatus)) {
            current = freshMatch(match.bowlingTeam(), match.battingTeam(), match.venue(),
                    match.maxOvers(), total + 1, 2, total);
        } else {
            current = updated;
        }
        return current;
    }

    public synchronized Match undo() {
        if (!history.isEmpty()) current = history.pop();
        return current;
    }

    public synchronized Match reset() {
        history.clear();
        current = freshMatch(current.battingTeam(), current.bowlingTeam(), current.venue(),
                current.maxOvers(), current.target(), current.inningsNumber(), current.previousInningsRuns());
        return current;
    }

    private static List<Player> updatePlayers(List<Player> players, String striker, int batRuns,
                                              String extraType, int extraRuns, boolean wicket) {
        return players.stream().map(player -> {
            if (!player.name().equals(striker)) return player;
            boolean facedBall = extraType == null || extraType.isBlank()
                    || "bye".equals(extraType) || "leg-bye".equals(extraType);
            int balls = player.balls() + (facedBall ? 1 : 0);
            int runs = player.runs() + batRuns;
            return new Player(player.name(), player.role(), runs, balls, player.wickets(),
                    player.fours() + (batRuns == 4 ? 1 : 0), player.sixes() + (batRuns == 6 ? 1 : 0),
                    wicket, strikeRate(runs, balls));
        }).toList();
    }

    private static String commentary(String striker, String bowler, int runs, String extra,
                                     int extraRuns, boolean wicket) {
        if (wicket) return bowler + " to " + striker + ", OUT!";
        if ("wide".equals(extra)) return bowler + " to " + striker + ", wide ball.";
        if ("no-ball".equals(extra)) return bowler + " to " + striker + ", no ball and " + runs + " runs.";
        if ("bye".equals(extra) || "leg-bye".equals(extra)) return bowler + " to " + striker + ", " + extraRuns + " " + extra + ".";
        return bowler + " to " + striker + ", " + (runs == 0 ? "no run" : runs + " run" + (runs == 1 ? "" : "s")) + ".";
    }

    private static String nextBatter(List<Player> players, String currentStriker, String nonStriker) {
        return players.stream().filter(p -> p.role().equals("Batter") && !p.name().equals(currentStriker)
                && !p.name().equals(nonStriker)
                && p.runs() == 0 && p.balls() == 0).findFirst().map(Player::name).orElse(currentStriker);
    }

    private static boolean validPlayer(String name, Match match) {
        return name != null && match.players().stream().anyMatch(p -> p.name().equals(name));
    }

    private static String status(int runs, int target, int wickets, int overs, int maxOvers) {
        return runs >= target ? "COMPLETED" : wickets >= 10 ? "ALL OUT"
                : overs >= maxOvers ? "INNINGS BREAK" : "LIVE";
    }

    private static String firstInningsStatus(int wickets, int overs, int maxOvers) {
        return wickets >= 10 ? "ALL OUT" : overs >= maxOvers ? "INNINGS BREAK" : "LIVE";
    }

    private static double strikeRate(int runs, int balls) {
        return balls == 0 ? 0 : Math.round(runs * 10000.0 / balls) / 100.0;
    }

    private static Match freshMatch(String batting, String bowling, String venue, int maxOvers, int target) {
        return freshMatch(batting, bowling, venue, maxOvers, target, 1, 0);
    }

    private static Match freshMatch(String batting, String bowling, String venue, int maxOvers,
                                    int target, int inningsNumber, int previousInningsRuns) {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 11; i++) players.add(new Player(batting + " player " + i, "Batter", 0, 0, 0, 0, 0, false, 0));
        for (int i = 1; i <= 5; i++) players.add(new Player(bowling + " bowler " + i, "Bowler", 0, 0, 0, 0, 0, false, 0));
        String striker = batting + " player 1";
        String nonStriker = batting + " player 2";
        String bowler = bowling + " bowler 1";
        return new Match(101, batting + " vs " + bowling, venue, "LIVE", batting, bowling, 0, 0, 0, 0,
                target, players, List.of(), 0, 0, 0, striker, nonStriker, bowler, maxOvers, List.of(),
                inningsNumber, previousInningsRuns, Instant.now());
    }
}
