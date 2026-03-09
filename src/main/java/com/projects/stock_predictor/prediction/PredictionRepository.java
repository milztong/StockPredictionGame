package com.projects.stock_predictor.prediction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    // All predictions for a specific user, newest first
    List<Prediction> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    // All pending predictions whose target date has arrived — used by the scheduler
    List<Prediction> findByStatusAndTargetDateLessThanEqual(
            Prediction.Status status, LocalDate date);
}