package com.projects.stock_predictor.result;

import com.projects.stock_predictor.prediction.Prediction;
import com.projects.stock_predictor.prediction.PredictionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ResolverScheduler {

    private final PredictionRepository predictionRepository;
    private final ResultService resultService;

    public ResolverScheduler(PredictionRepository predictionRepository,
                             ResultService resultService) {
        this.predictionRepository = predictionRepository;
        this.resultService = resultService;
    }

    /**
     * Runs every day at 18:00 (after US markets close at 16:00 EST).
     * Finds all PENDING predictions whose target date has passed and resolves them.
     */
    @Scheduled(cron = "0 0 18 * * *")
    public void resolveExpiredPredictions() {
        List<Prediction> due = predictionRepository
                .findByStatusAndTargetDateLessThanEqual(Prediction.Status.PENDING, LocalDate.now());

        System.out.printf("Scheduler: found %d predictions to resolve%n", due.size());
        due.forEach(resultService::resolve);
    }
}