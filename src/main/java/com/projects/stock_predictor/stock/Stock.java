package com.projects.stock_predictor.stock;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "last_fetched")
    private LocalDateTime lastFetched;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Stock(String ticker, String companyName) {
        this.ticker = ticker;
        this.companyName = companyName;
    }
}