package com.projects.stock_predictor.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, UUID> {

    // Get prices for a stock within a date range, sorted ascending
    List<StockPrice> findByStockIdAndDateBetweenOrderByDateAsc(
            UUID stockId, LocalDate from, LocalDate to);

    // Check if we already have data for a specific date
    boolean existsByStockIdAndDate(UUID stockId, LocalDate date);
}
