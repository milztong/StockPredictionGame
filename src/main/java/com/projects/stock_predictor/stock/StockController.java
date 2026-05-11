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

    @GetMapping("/daily")
    public ResponseEntity<StockService.AnonymousStockResponse> getDailyStock() {
        try {
            return ResponseEntity.ok(stockService.getDailyChallengeStock());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

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

    @GetMapping("/{stockId}/history")
    public ResponseEntity<List<StockService.PricePoint>> getFullHistory(@PathVariable UUID stockId) {
        return ResponseEntity.ok(stockService.getFullHistory(stockId));
    }
}
