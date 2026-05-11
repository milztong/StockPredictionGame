package com.projects.stock_predictor.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, UUID> {

    List<StockPrice> findByStockIdAndDateBetweenOrderByDateAsc(
            UUID stockId, LocalDate from, LocalDate to);

    // Ersetzt 90x existsByStockIdAndDate — 1 Query statt N Queries
    @Query("SELECT sp.date FROM StockPrice sp WHERE sp.stock.id = :stockId AND sp.date >= :from")
    Set<LocalDate> findExistingDatesByStockIdSince(@Param("stockId") UUID stockId,
                                                   @Param("from") LocalDate from);
}
