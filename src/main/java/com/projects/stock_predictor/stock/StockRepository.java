package com.projects.stock_predictor.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByTicker(String ticker);

    // Pick a random active stock from the DB
    @Query(value = "SELECT * FROM stocks WHERE is_active = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Stock> findRandomActiveStock();
}
