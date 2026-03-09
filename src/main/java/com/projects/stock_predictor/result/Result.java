package com.projects.stock_predictor.result;

import com.projects.stock_predictor.prediction.Prediction;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "results")
@Data
@NoArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private Prediction prediction;

    @Column(name = "actual_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualPrice;

    @Column(name = "direction_correct", nullable = false)
    private boolean directionCorrect;

    @Column(name = "accuracy_score", nullable = false)
    private int accuracyScore; // 0–100

    @Column(name = "direction_score", nullable = false)
    private int directionScore; // 0 or 50

    @Column(name = "total_score", nullable = false)
    private int totalScore; // accuracyScore + directionScore, max 150

    @Column(name = "resolved_at", nullable = false)
    private LocalDateTime resolvedAt;
}