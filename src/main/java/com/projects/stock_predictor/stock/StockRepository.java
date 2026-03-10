package com.projects.stock_predictor.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByTicker(String ticker);

    // Used by daily challenge — returns all active stocks, ordered for deterministic selection
    List<Stock> findAllByActiveTrueOrderByTickerAsc();

    // Alias used in StockService
    default List<Stock> findAllByActiveTrue() {
        return findAllByActiveTrueOrderByTickerAsc();
    }
}
