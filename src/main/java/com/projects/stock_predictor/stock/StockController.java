package com.projects.stock_predictor.stock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * GET /api/stocks/daily
     * Returns today's challenge stock — same for all users.
     * Rotates automatically every day.
     */
    @GetMapping("/daily")
    public ResponseEntity<StockService.AnonymousStockResponse> getDailyStock() {
        try {
            return ResponseEntity.ok(stockService.getDailyChallengeStock());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/stocks/add?ticker=AAPL
     * Dynamically adds a stock by ticker — no hardcoded list needed.
     */
    @PostMapping("/add")
    public ResponseEntity<String> addStock(
            @RequestParam String ticker,
            @RequestParam(required = false) String name) {
        try {
            stockService.addStockByTicker(ticker.toUpperCase(), name);
            return ResponseEntity.ok("Added: " + ticker.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Alpha Vantage error: " + e.getMessage());
        }
    }

    /**
     * GET /api/stocks/{stockId}/history
     * Full price history — used on reveal page.
     */
    @GetMapping("/{stockId}/history")
    public ResponseEntity<List<StockService.PricePoint>> getFullHistory(@PathVariable UUID stockId) {
        return ResponseEntity.ok(stockService.getFullHistory(stockId));
    }
}
