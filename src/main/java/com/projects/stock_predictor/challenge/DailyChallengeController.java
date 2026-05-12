package com.projects.stock_predictor.challenge;

import com.projects.stock_predictor.stock.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/challenge")
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;
    private final StockService stockService;

    @Value("${admin.trigger.secret}")
    private String triggerSecret;

    public DailyChallengeController(DailyChallengeService dailyChallengeService,
                                    StockService stockService) {
        this.dailyChallengeService = dailyChallengeService;
        this.stockService = stockService;
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayChallenge() {
        try {
            return ResponseEntity.ok(stockService.getDailyChallengeStock());
        } catch (IOException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        }
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerManually(@RequestParam String secret) {
        if (!triggerSecret.equals(secret)) {
            return ResponseEntity.status(403).body("Ungültiges Secret.");
        }
        LocalDate today = LocalDate.now();
        try {
            dailyChallengeService.setupTodayChallenge(today);
            dailyChallengeService.fetchResolutionPrices(today);
            dailyChallengeService.resolveExpiredPredictions(today);
            return ResponseEntity.ok("Scheduler erfolgreich ausgeführt für " + today);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler: " + e.getMessage());
        }
    }
}
