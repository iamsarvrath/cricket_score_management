package com.cricket.livescore.controller;

import com.cricket.livescore.model.Match;
import com.cricket.livescore.service.MatchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
@Validated
public class MatchController {
    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/matches/live")
    public Match liveMatch() {
        return matchService.getCurrentMatch();
    }

    @PostMapping("/matches/start")
    public Match startMatch(
            @RequestParam @NotBlank String battingTeam,
            @RequestParam @NotBlank String bowlingTeam,
            @RequestParam(defaultValue = "Local stadium") String venue,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int maxOvers,
            @RequestParam(defaultValue = "200") @Min(1) int target) {
        return matchService.startMatch(battingTeam, bowlingTeam, venue, maxOvers, target);
    }

    @GetMapping("/matches/{id}/summary")
    public ResponseEntity<?> summary(@PathVariable long id) {
        Match match = matchService.getCurrentMatch();
        if (match.id() != id) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "match", match.title(),
                "innings", match.inningsNumber(),
                "result", match.status().equals("LIVE")
                        ? match.battingTeam() + " need " + Math.max(0, match.target() - match.runs()) + " runs"
                        : match.status(),
                "score", match.score(),
                "overs", match.overs() + "." + match.balls(),
                "target", match.target()
        ));
    }

    @PostMapping("/matches/live/runs")
    public Match addRuns(@RequestParam @Min(0) @Max(6) int runs,
                         @RequestParam(required = false) String striker,
                         @RequestParam(required = false) String bowler) {
        return matchService.recordDelivery(runs, null, 0, false, striker, bowler);
    }

    @PostMapping("/matches/live/wicket")
    public Match addWicket(@RequestParam(required = false) String striker,
                           @RequestParam(required = false) String bowler) {
        return matchService.recordDelivery(0, null, 0, true, striker, bowler);
    }

    @PostMapping("/matches/live/extras/{type}")
    public Match addExtra(@PathVariable String type,
                          @RequestParam(required = false) String striker,
                          @RequestParam(required = false) String bowler) {
        return matchService.recordDelivery(0, type, 1, false, striker, bowler);
    }

    @PostMapping("/matches/live/delivery")
    public Match delivery(
            @RequestParam(defaultValue = "0") @Min(0) @Max(6) int runs,
            @RequestParam(defaultValue = "") String extra,
            @RequestParam(defaultValue = "0") @Min(0) @Max(6) int extraRuns,
            @RequestParam(defaultValue = "false") boolean wicket,
            @RequestParam(required = false) String striker,
            @RequestParam(required = false) String bowler) {
        return matchService.recordDelivery(runs, extra.isBlank() ? null : extra, extraRuns,
                wicket, striker, bowler);
    }

    @PostMapping("/matches/live/undo")
    public Match undo() {
        return matchService.undo();
    }

    @PostMapping("/matches/live/reset")
    public Match reset() {
        return matchService.reset();
    }

    @GetMapping("/matches")
    public List<Map<String, Object>> matches() {
        Match live = matchService.getCurrentMatch();
        return List.of(
                Map.of("id", live.id(), "title", live.title(), "status", live.status(), "score", live.score()),
                Map.of("id", 102, "title", "England vs Pakistan", "status", "UPCOMING", "score", "Starts 20:00"),
                Map.of("id", 103, "title", "South Africa vs New Zealand", "status", "RESULT", "score", "SA won by 7 wickets")
        );
    }
}
