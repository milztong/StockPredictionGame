package com.projects.stock_predictor.challenge;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DailyScheduler {

    private final DailyChallengeService dailyChallengeService;

    public DailyScheduler(DailyChallengeService dailyChallengeService) {
        this.dailyChallengeService = dailyChallengeService;
    }

    @Scheduled(cron = "0 5 6 * * *")
    public void runDailyJob() {
        LocalDate today = LocalDate.now();
        System.out.println("[DailyScheduler] === Gestartet für " + today + " ===");

        dailyChallengeService.setupTodayChallenge(today);
        dailyChallengeService.fetchResolutionPrices(today);
        dailyChallengeService.resolveExpiredPredictions(today);

        System.out.println("[DailyScheduler] === Abgeschlossen ===");
    }
}
