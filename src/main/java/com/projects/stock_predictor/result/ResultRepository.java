package com.projects.stock_predictor.result;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {

    Optional<Result> findByPredictionId(UUID predictionId);

    // Eagerly fetch prediction to avoid N+1 queries in leaderboard
    @Query("SELECT r FROM Result r JOIN FETCH r.prediction p JOIN FETCH p.stock")
    List<Result> findAllWithPredictions();
}