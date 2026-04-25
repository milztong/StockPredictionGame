package com.projects.stock_predictor.result;

import com.projects.stock_predictor.prediction.Prediction;
import com.projects.stock_predictor.prediction.PredictionRepository;
import com.projects.stock_predictor.stock.StockPrice;
import com.projects.stock_predictor.stock.StockPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final PredictionRepository predictionRepository;
    private final StockPriceRepository stockPriceRepository;

    public ResultService(ResultRepository resultRepository,
                         PredictionRepository predictionRepository,
                         StockPriceRepository stockPriceRepository) {
        this.resultRepository = resultRepository;
        this.predictionRepository = predictionRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    /**
     * Resolves a single prediction — calculates score and saves result.
     * Called by the scheduler, or can be called manually for testing.
     */
    @Transactional
    public void resolve(Prediction prediction) {
        // Find the actual closing price on the target date
        var pricesOnTargetDate = stockPriceRepository
                .findByStockIdAndDateBetweenOrderByDateAsc(
                        prediction.getStock().getId(),
                        prediction.getTargetDate().minusDays(3), // buffer for weekends/holidays
                        prediction.getTargetDate()
                );

        if (pricesOnTargetDate.isEmpty()) {
            System.out.println("No price data yet for prediction " + prediction.getId() + " — skipping");
            return;
        }

        StockPrice actualStockPrice = pricesOnTargetDate.getLast();
        BigDecimal actualPrice = actualStockPrice.getClose();

        // Direction: was user's guess correct?
        boolean actualWentUp = actualPrice.compareTo(prediction.getBasePrice()) > 0;
        boolean directionCorrect = (prediction.getDirection() == Prediction.Direction.UP) == actualWentUp;

        // Scoring
        int directionScore = directionCorrect ? 50 : 0;
        double percentError = Math.abs(
                prediction.getPredictedPrice().subtract(actualPrice)
                        .divide(actualPrice, 4, java.math.RoundingMode.HALF_UP)
                        .doubleValue()
        ) * 100;
        int accuracyScore = (int) Math.max(0, 100 - (percentError * 10));
        int totalScore = directionScore + accuracyScore;

        // Save result
        Result result = new Result();
        result.setPrediction(prediction);
        result.setActualPrice(actualPrice);
        result.setDirectionCorrect(directionCorrect);
        result.setDirectionScore(directionScore);
        result.setAccuracyScore(accuracyScore);
        result.setTotalScore(totalScore);
        result.setResolvedAt(LocalDateTime.now());
        resultRepository.save(result);

        // Mark prediction as resolved
        prediction.setStatus(Prediction.Status.RESOLVED);
        predictionRepository.save(prediction);

        System.out.printf("Resolved prediction %s — score: %d/150%n", prediction.getId(), totalScore);
    }

    /**
     * GET /api/results/{predictionId}
     * Returns the result for a prediction (only available once resolved).
     */
    public ResultResponse getResult(UUID predictionId) {
        Result result = resultRepository.findByPredictionId(predictionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Result not yet available for prediction: " + predictionId));

        Prediction p = result.getPrediction();
        return new ResultResponse(
                result.getId(),
                predictionId,
                p.getStock().getId(),
                p.getStock().getTicker(),       // revealed now!
                p.getStock().getCompanyName(),  // revealed now!
                p.getStockCodename(),
                p.getPredictedPrice(),
                p.getDirection(),
                p.getBasePrice(),
                result.getActualPrice(),
                result.isDirectionCorrect(),
                result.getAccuracyScore(),
                result.getDirectionScore(),
                result.getTotalScore(),
                result.getResolvedAt().toString()
        );
    }

    public record ResultResponse(
            UUID resultId,
            UUID predictionId,
            UUID stockId,
            String ticker,           // now revealed
            String companyName,      // now revealed
            String codename,
            java.math.BigDecimal predictedPrice,
            Prediction.Direction direction,
            java.math.BigDecimal basePrice,
            java.math.BigDecimal actualPrice,
            boolean directionCorrect,
            int accuracyScore,
            int directionScore,
            int totalScore,
            String resolvedAt
    ) {}
}