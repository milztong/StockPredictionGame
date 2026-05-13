package com.projects.stock_predictor.challenge;

import com.projects.stock_predictor.prediction.Prediction;
import com.projects.stock_predictor.prediction.PredictionRepository;
import com.projects.stock_predictor.result.ResultService;
import com.projects.stock_predictor.stock.AlphaVantageClient;
import com.projects.stock_predictor.stock.Stock;
import com.projects.stock_predictor.stock.StockPrice;
import com.projects.stock_predictor.stock.StockPriceRepository;
import com.projects.stock_predictor.stock.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DailyChallengeService {

    private static final int HISTORY_DAYS = 90;
    private static final int RECENT_STOCKS_EXCLUSION = 14;

    private final DailyChallengeRepository dailyChallengeRepository;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PredictionRepository predictionRepository;
    private final AlphaVantageClient alphaVantageClient;
    private final ResultService resultService;

    public DailyChallengeService(DailyChallengeRepository dailyChallengeRepository,
                                 StockRepository stockRepository,
                                 StockPriceRepository stockPriceRepository,
                                 PredictionRepository predictionRepository,
                                 AlphaVantageClient alphaVantageClient,
                                 ResultService resultService) {
        this.dailyChallengeRepository = dailyChallengeRepository;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.predictionRepository = predictionRepository;
        this.alphaVantageClient = alphaVantageClient;
        this.resultService = resultService;
    }

    @Transactional
    public void setupTodayChallenge(LocalDate today) {
        if (dailyChallengeRepository.findByChallengeDate(today).isPresent()) {
            log("Challenge für heute existiert bereits — überspringe.");
            return;
        }

        List<Stock> allStocks = stockRepository.findAllByActiveTrue();
        if (allStocks.isEmpty()) {
            log("WARNUNG: Keine aktiven Aktien in der DB.");
            return;
        }

        Set<UUID> recentIds = Set.copyOf(
                dailyChallengeRepository.findRecentStockIds(RECENT_STOCKS_EXCLUSION));

        List<Stock> candidates = allStocks.stream()
                .filter(s -> !recentIds.contains(s.getId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log("Alle Aktien kürzlich verwendet — nehme aus dem vollen Pool.");
            candidates = allStocks;
        }

        Stock chosen = candidates.get(new Random().nextInt(candidates.size()));
        log("Gewählte Aktie für heute: " + chosen.getTicker());

        try {
            fetchAndStorePrices(chosen);
            dailyChallengeRepository.save(new DailyChallenge(today, chosen));
            log("Challenge für " + today + " erstellt: " + chosen.getTicker());
        } catch (IOException e) {
            log("FEHLER beim Holen der Preisdaten für " + chosen.getTicker() + ": " + e.getMessage());
        }
    }

    @Transactional
    public void fetchResolutionPrices(LocalDate today) {
        LocalDate targetDay = today.minusDays(7);
        List<DailyChallenge> toResolve = dailyChallengeRepository.findUnresolvedBefore(targetDay);

        if (toResolve.isEmpty()) {
            log("Keine fälligen Challenges zum Auflösen.");
            return;
        }

        log("Hole Auflösungspreise für " + toResolve.size() + " Challenge(s)...");
        for (DailyChallenge challenge : toResolve) {
            Stock stock = challenge.getStock();
            boolean alreadyFetched = stockPriceRepository
                    .findByStockIdAndDateBetweenOrderByDateAsc(stock.getId(), today.minusDays(1), today)
                    .stream().anyMatch(p -> !p.getDate().isBefore(today.minusDays(1)));

            if (!alreadyFetched) {
                try {
                    fetchAndStorePrices(stock);
                    log("Auflösungspreis geholt für: " + stock.getTicker());
                } catch (IOException e) {
                    log("FEHLER Auflösungspreis " + stock.getTicker() + ": " + e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void resolveExpiredPredictions(LocalDate today) {
        List<Prediction> due = predictionRepository
                .findByStatusAndTargetDateLessThanEqual(Prediction.Status.PENDING, today);

        if (due.isEmpty()) {
            log("Keine fälligen Predictions.");
            return;
        }

        log("Löse " + due.size() + " Prediction(s) auf...");
        due.forEach(prediction -> {
            try {
                resultService.resolve(prediction);
            } catch (Exception e) {
                log("FEHLER Prediction " + prediction.getId() + ": " + e.getMessage());
            }
        });

        LocalDate cutoff = today.minusDays(7);
        List<DailyChallenge> done = dailyChallengeRepository.findUnresolvedBefore(cutoff);
        done.forEach(c -> c.setResolved(true));
        dailyChallengeRepository.saveAll(done);
    }

    private void fetchAndStorePrices(Stock stock) throws IOException {
        List<AlphaVantageClient.PriceData> prices = alphaVantageClient.fetchDailyPrices(stock.getTicker());
        LocalDate from = LocalDate.now().minusDays(HISTORY_DAYS);
        Set<LocalDate> existingDates = stockPriceRepository
                .findExistingDatesByStockIdSince(stock.getId(), from);

        List<StockPrice> toSave = prices.stream()
                .filter(data -> !existingDates.contains(data.date()))
                .map(data -> new StockPrice(stock, data.date(), data.open(),
                        data.high(), data.low(), data.close(), data.volume()))
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            stockPriceRepository.saveAll(toSave);
            log("Gespeichert: " + toSave.size() + " neue Preispunkte für " + stock.getTicker());
        }
    }

    private void log(String msg) {
        System.out.println("[DailyChallengeService] " + msg);
    }
}
