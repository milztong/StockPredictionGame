package com.projects.stock_predictor.prediction;

import com.projects.stock_predictor.stock.Stock;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "predictions")
@Data
@NoArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Will be linked to a real User in Phase 3 — for now just a placeholder string
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "predicted_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice; // Price at time of prediction (last visible close)

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate; // Always submitted_at + 7 days

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "stock_codename", nullable = false)
    private String stockCodename; // e.g. "Silent Falcon" — stored for display

    public enum Direction { UP, DOWN }
    public enum Status { PENDING, RESOLVED }
}