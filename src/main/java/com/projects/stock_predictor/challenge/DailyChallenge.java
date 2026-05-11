package com.projects.stock_predictor.challenge;

import com.projects.stock_predictor.stock.Stock;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents the stock challenge for a specific calendar day.
 * Created by DailyScheduler every morning — never triggered by a user request.
 * challenge_date  → der Tag der Challenge (z.B. 2025-05-08)
 * stock           → welche Aktie heute dran ist
 * resolved        → true sobald der Preis nach 7 Tagen geholt wurde und Predictions aufgelöst sind
 */
@Entity
@Table(name = "daily_challenges",
       uniqueConstraints = @UniqueConstraint(columnNames = "challenge_date"))
@Data
@NoArgsConstructor
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "challenge_date", nullable = false, unique = true)
    private LocalDate challengeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    public DailyChallenge(LocalDate challengeDate, Stock stock) {
        this.challengeDate = challengeDate;
        this.stock = stock;
    }
}
