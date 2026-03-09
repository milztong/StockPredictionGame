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
     * GET /api/stocks/random
     * Returns an anonymized stock with 83 days of price history.
     * The ticker and company name are never included in the response.
     */
    @GetMapping("/random")
    public ResponseEntity<StockService.AnonymousStockResponse> getRandomStock() {
        try {
            return ResponseEntity.ok(stockService.getRandomAnonymousStock());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/stocks/{stockId}/history
     * Returns full price history including last 7 days.
     * Used on the reveal page after prediction is submitted.
     */
    @GetMapping("/{stockId}/history")
    public ResponseEntity<List<StockService.PricePoint>> getFullHistory(@PathVariable UUID stockId) {
        return ResponseEntity.ok(stockService.getFullHistory(stockId));
    }
}
