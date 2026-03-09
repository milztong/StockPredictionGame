package com.projects.stock_predictor.prediction;

import com.projects.stock_predictor.stock.Stock;
import com.projects.stock_predictor.stock.StockPrice;
import com.projects.stock_predictor.stock.StockPriceRepository;
import com.projects.stock_predictor.stock.StockRepository;
import com.projects.stock_predictor.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final StockService stockService;

    public PredictionService(PredictionRepository predictionRepository,
                             StockRepository stockRepository,
                             StockPriceRepository stockPriceRepository,
                             StockService stockService) {
        this.predictionRepository = predictionRepository;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.stockService = stockService;
    }

    @Transactional
    public PredictionResponse submit(SubmitPredictionRequest request) {
        Stock stock = stockRepository.findById(request.stockId())
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + request.stockId()));

        // Base price = the most recent visible close price (7 days ago)
        LocalDate basePriceDate = LocalDate.now().minusDays(7);
        List<StockPrice> recentPrices = stockPriceRepository
                .findByStockIdAndDateBetweenOrderByDateAsc(stock.getId(),
                        basePriceDate.minusDays(5), basePriceDate);

        if (recentPrices.isEmpty()) {
            throw new IllegalStateException("No price data available for stock: " + request.stockId());
        }

        // Use the most recent available price as the base
        StockPrice baseStockPrice = recentPrices.getLast();

        Prediction prediction = new Prediction();
        prediction.setUserId(request.userId());
        prediction.setStock(stock);
        prediction.setPredictedPrice(request.predictedPrice());
        prediction.setDirection(request.direction());
        prediction.setBasePrice(baseStockPrice.getClose());
        prediction.setTargetDate(LocalDate.now().plusDays(7));
        prediction.setSubmittedAt(LocalDateTime.now());
        prediction.setStatus(Prediction.Status.PENDING);
        prediction.setStockCodename(stockService.generateCodename(stock.getId()));

        Prediction saved = predictionRepository.save(prediction);
        return PredictionResponse.from(saved);
    }

    public List<PredictionResponse> getMyPredictions(UUID userId) {
        return predictionRepository.findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(PredictionResponse::from)
                .toList();
    }

    public PredictionResponse getPrediction(UUID predictionId) {
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + predictionId));
        return PredictionResponse.from(prediction);
    }

    // ── Response DTO ──────────────────────────────────────────────────────────

    public record PredictionResponse(
            UUID id,
            UUID stockId,
            String stockCodename,
            java.math.BigDecimal predictedPrice,
            Prediction.Direction direction,
            java.math.BigDecimal basePrice,
            String targetDate,
            String submittedAt,
            Prediction.Status status
    ) {
        public static PredictionResponse from(Prediction p) {
            return new PredictionResponse(
                    p.getId(),
                    p.getStock().getId(),
                    p.getStockCodename(),
                    p.getPredictedPrice(),
                    p.getDirection(),
                    p.getBasePrice(),
                    p.getTargetDate().toString(),
                    p.getSubmittedAt().toString(),
                    p.getStatus()
            );
        }
    }
}