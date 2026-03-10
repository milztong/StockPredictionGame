package com.projects.stock_predictor.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StockService {

    private static final int CACHE_TTL_HOURS = 24;
    private static final int HISTORY_DAYS = 90;

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final AlphaVantageClient alphaVantageClient;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alphavantage.api.key}")
    private String apiKey;

    public StockService(StockRepository stockRepository,
                        StockPriceRepository stockPriceRepository,
                        AlphaVantageClient alphaVantageClient,
                        OkHttpClient httpClient) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.alphaVantageClient = alphaVantageClient;
        this.httpClient = httpClient;
    }

    /**
     * Returns the daily challenge stock — same for all users on the same day.
     * Uses the date as a seed so it rotates deterministically every day.
     */
    @Transactional
    public AnonymousStockResponse getDailyChallengeStock() throws IOException {
        List<Stock> activeStocks = stockRepository.findAllByActiveTrue();
        if (activeStocks.isEmpty()) {
            throw new RuntimeException("No active stocks in DB. Add some via POST /api/stocks/add");
        }

        // Use today's date as seed — same stock all day, rotates daily
        int dayOfYear = LocalDate.now().getDayOfYear();
        int year = LocalDate.now().getYear();
        int seed = year * 1000 + dayOfYear;
        Stock stock = activeStocks.get(seed % activeStocks.size());

        refreshCacheIfNeeded(stock);

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
                visibleUntil.plusDays(7).toString()
        );
    }

    /**
     * Dynamically adds a stock by ticker using Alpha Vantage symbol search.
     */
    @Transactional
    public void addStockByTicker(String ticker, String name) throws IOException {
        if (stockRepository.findByTicker(ticker).isPresent()) {
            throw new IllegalArgumentException("Stock already exists: " + ticker);
        }

        String companyName = name;

        // Only call Alpha Vantage search if no name was provided
        if (companyName == null || companyName.isBlank()) {
            String url = "https://www.alphavantage.co/query"
                    + "?function=SYMBOL_SEARCH"
                    + "&keywords=" + ticker
                    + "&apikey=" + apiKey;

            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Alpha Vantage search failed");
                }
                String json = response.body().string();
                JsonNode root = objectMapper.readTree(json);
                JsonNode matches = root.get("bestMatches");

                if (matches == null || matches.isEmpty()) {
                    throw new IllegalArgumentException("Ticker not found: " + ticker);
                }

                companyName = ticker; // fallback
                for (JsonNode match : matches) {
                    if (match.get("1. symbol").asText().equalsIgnoreCase(ticker)) {
                        companyName = match.get("2. name").asText();
                        break;
                    }
                }
            }
        }

        stockRepository.save(new Stock(ticker, companyName));
        System.out.println("Added stock: " + ticker + " — " + companyName);
    }

    /**
     * Returns full price history including last 7 days — used on reveal page.
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
            System.out.println("Cache stale for " + stock.getTicker() + ", fetching...");
            List<AlphaVantageClient.PriceData> prices = alphaVantageClient.fetchDailyPrices(stock.getTicker());

            for (AlphaVantageClient.PriceData data : prices) {
                if (!stockPriceRepository.existsByStockIdAndDate(stock.getId(), data.date())) {
                    stockPriceRepository.save(new StockPrice(
                            stock, data.date(), data.open(), data.high(),
                            data.low(), data.close(), data.volume()
                    ));
                }
            }

            stock.setLastFetched(LocalDateTime.now());
            stockRepository.save(stock);
            System.out.println("Cache refreshed for " + stock.getTicker());
        }
    }

    public String generateCodename(UUID stockId) {
        String[] adjectives = {"Silent", "Golden", "Iron", "Silver", "Phantom",
                "Neon", "Cosmic", "Electric", "Turbo", "Apex"};
        String[] nouns = {"Falcon", "Titan", "Nova", "Orbit", "Pulse",
                "Vortex", "Comet", "Nexus", "Spark", "Zenith"};
        int hash = Math.abs(stockId.hashCode());
        return adjectives[hash % adjectives.length] + " " + nouns[(hash / 10) % nouns.length];
    }

    // ── Response DTOs ────────────────────────────────────────────────────────

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