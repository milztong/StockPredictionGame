package com.projects.stock_predictor.leaderboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * GET /api/leaderboard
     * Public endpoint — no auth required.
     * Returns top 50 users sorted by total score.
     */
    @GetMapping
    public ResponseEntity<List<LeaderboardService.LeaderboardEntry>> getLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getTopLeaderboard());
    }
}
