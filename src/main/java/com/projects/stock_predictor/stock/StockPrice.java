package com.projects.stock_predictor.stock;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stock_prices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stock_id", "date"})
})
@Data
@NoArgsConstructor
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal open;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal high;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal low;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    public StockPrice(Stock stock, LocalDate date, BigDecimal open,
                      BigDecimal high, BigDecimal low, BigDecimal close, Long volume) {
        this.stock = stock;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}