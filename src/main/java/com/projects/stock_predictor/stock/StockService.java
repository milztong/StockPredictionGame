package com.projects.stock_predictor.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StockService {

    // How many hours before we re-fetch from Alpha Vantage
    private static final int CACHE_TTL_HOURS = 24;

    // How many days of history to show the user (last 7 are hidden for prediction)
    private static final int HISTORY_DAYS = 90;

    // Curated pool of well-known stocks — add more as you like
    private static final List<String[]> STOCK_POOL = List.of(
            new String[]{"AAPL", "Apple Inc."},
            new String[]{"MSFT", "Microsoft Corporation"},
            new String[]{"GOOGL", "Alphabet Inc."},
            new String[]{"AMZN", "Amazon.com Inc."},
            new String[]{"TSLA", "Tesla Inc."},
            new String[]{"META", "Meta Platforms Inc."},
            new String[]{"NVDA", "NVIDIA Corporation"},
            new String[]{"NFLX", "Netflix Inc."},
            new String[]{"AMD", "Advanced Micro Devices"},
            new String[]{"INTC", "Intel Corporation"}
    );

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final AlphaVantageClient alphaVantageClient;

    public StockService(StockRepository stockRepository,
                        StockPriceRepository stockPriceRepository,
                        AlphaVantageClient alphaVantageClient) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.alphaVantageClient = alphaVantageClient;
    }

    /**
     * Seeds the DB with the curated stock pool if not already present.
     * Call this once on startup.
     */
    @Transactional
    public void seedStocksIfNeeded() {
        for (String[] entry : STOCK_POOL) {
            String ticker = entry[0];
            String name = entry[1];
            if (stockRepository.findByTicker(ticker).isEmpty()) {
                stockRepository.save(new Stock(ticker, name));
                System.out.println("Seeded stock: " + ticker);
            }
        }
    }

    /**
     * Returns a random stock with 83 days of OHLC history (last 7 days hidden).
     * Fetches from Alpha Vantage if cache is stale.
     */
    @Transactional
    public AnonymousStockResponse getRandomAnonymousStock() throws IOException {
        Stock stock = stockRepository.findRandomActiveStock()
                .orElseThrow(() -> new RuntimeException("No active stocks found in DB"));

        refreshCacheIfNeeded(stock);

        // Show 83 days of history — last 7 days are hidden until prediction reveal
        LocalDate today = LocalDate.now();
        LocalDate visibleFrom = today.minusDays(HISTORY_DAYS);
        LocalDate visibleUntil = today.minusDays(7);

        List<StockPrice> prices = stockPriceRepository
                .findByStockIdAndDateBetweenOrderByDateAsc(stock.getId(), visibleFrom, visibleUntil);

        List<PricePoint> pricePoints = prices.stream()
                .map(p -> new PricePoint(
                        p.getDate().toString(),
                        p.getOpen(),
                        p.getHigh(),
                        p.getLow(),
                        p.getClose(),
                        p.getVolume()
                ))
                .toList();

        return new AnonymousStockResponse(
                stock.getId(),
                generateCodename(stock.getId()),
                pricePoints,
                visibleUntil.plusDays(7).toString() // target date = 7 days after last visible
        );
    }

    /**
     * Returns full price history for a stock including the last 7 days.
     * Used after prediction is revealed.
     */
    @Transactional
    public List<PricePoint> getFullHistory(UUID stockId) {
        LocalDate from = LocalDate.now().minusDays(HISTORY_DAYS);
        LocalDate to = LocalDate.now();

        return stockPriceRepository
                .findByStockIdAndDateBetweenOrderByDateAsc(stockId, from, to)
                .stream()
                .map(p -> new PricePoint(
                        p.getDate().toString(),
                        p.getOpen(),
                        p.getHigh(),
                        p.getLow(),
                        p.getClose(),
                        p.getVolume()
                ))
                .toList();
    }

    private void refreshCacheIfNeeded(Stock stock) throws IOException {
        boolean isStale = stock.getLastFetched() == null ||
                stock.getLastFetched().isBefore(LocalDateTime.now().minusHours(CACHE_TTL_HOURS));

        if (isStale) {
            System.out.println("Cache stale for " + stock.getTicker() + ", fetching from Alpha Vantage...");
            List<AlphaVantageClient.PriceData> prices = alphaVantageClient.fetchDailyPrices(stock.getTicker());

            for (AlphaVantageClient.PriceData data : prices) {
                if (!stockPriceRepository.existsByStockIdAndDate(stock.getId(), data.date())) {
                    stockPriceRepository.save(new StockPrice(
                            stock,
                            data.date(),
                            data.open(),
                            data.high(),
                            data.low(),
                            data.close(),
                            data.volume()
                    ));
                }
            }

            stock.setLastFetched(LocalDateTime.now());
            stockRepository.save(stock);
            System.out.println("Cache refreshed for " + stock.getTicker());
        }
    }

    /**
     * Generates a consistent codename from the stock UUID.
     * Same stock always gets same codename — but it reveals nothing about the ticker.
     */
    public String generateCodename(UUID stockId) {
        String[] adjectives = {"Silent", "Golden", "Iron", "Silver", "Phantom", "Neon", "Cosmic", "Electric", "Turbo", "Apex"};
        String[] nouns = {"Falcon", "Titan", "Nova", "Orbit", "Pulse", "Vortex", "Comet", "Nexus", "Spark", "Zenith"};
        int hash = Math.abs(stockId.hashCode());
        return adjectives[hash % adjectives.length] + " " + nouns[(hash / 10) % nouns.length];
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    public record AnonymousStockResponse(
            UUID stockId,
            String codename,
            List<PricePoint> prices,
            String targetDate
    ) {}

    public record PricePoint(
            String date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            Long volume
    ) {}
}