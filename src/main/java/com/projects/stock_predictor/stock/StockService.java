package com.projects.stock_predictor.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projects.stock_predictor.challenge.DailyChallenge;
import com.projects.stock_predictor.challenge.DailyChallengeRepository;
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

    private static final int HISTORY_DAYS = 90;

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final DailyChallengeRepository dailyChallengeRepository;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alphavantage.api.key}")
    private String apiKey;

    public StockService(StockRepository stockRepository,
                        StockPriceRepository stockPriceRepository,
                        DailyChallengeRepository dailyChallengeRepository,
                        OkHttpClient httpClient) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.dailyChallengeRepository = dailyChallengeRepository;
        this.httpClient = httpClient;
    }

    /**
     * Gibt die heutige Challenge zurück — immer aus der DB, nie ein API-Call.
     * Der DailyScheduler hat die Daten morgens bereits vorbereitet.
     */
    @Transactional(readOnly = true)
    public AnonymousStockResponse getDailyChallengeStock() throws IOException {
        LocalDate today = LocalDate.now();

        DailyChallenge challenge = dailyChallengeRepository.findByChallengeDate(today)
                .orElseThrow(() -> new IOException(
                        "Heutige Challenge noch nicht bereit. Der Scheduler läuft um 06:00 UTC."));

        Stock stock = challenge.getStock();

        LocalDate visibleFrom = today.minusDays(HISTORY_DAYS);
        LocalDate visibleUntil = today.minusDays(7); // Letzte 7 Tage werden erst nach Auflösung sichtbar

        List<StockPrice> prices = stockPriceRepository
                .findByStockIdAndDateBetweenOrderByDateAsc(stock.getId(), visibleFrom, visibleUntil);

        if (prices.isEmpty()) {
            throw new IOException("Keine Preisdaten für die heutige Challenge verfügbar.");
        }

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
     * Fügt eine Aktie zur DB hinzu (nur Metadaten — kein Preisfetch hier).
     * Preisdaten werden beim ersten Scheduler-Lauf geholt, wenn diese Aktie ausgewählt wird.
     */
    @Transactional
    public void addStockByTicker(String ticker, String name) throws IOException {
        if (stockRepository.findByTicker(ticker).isPresent()) {
            throw new IllegalArgumentException("Aktie existiert bereits: " + ticker);
        }

        String companyName = name;

        if (companyName == null || companyName.isBlank()) {
            String url = "https://www.alphavantage.co/query"
                    + "?function=SYMBOL_SEARCH"
                    + "&keywords=" + ticker
                    + "&apikey=" + apiKey;

            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Alpha Vantage Suche fehlgeschlagen");
                }
                String json = response.body().string();
                JsonNode root = objectMapper.readTree(json);
                JsonNode matches = root.get("bestMatches");

                if (matches == null || matches.isEmpty()) {
                    throw new IllegalArgumentException("Ticker nicht gefunden: " + ticker);
                }

                companyName = ticker; // Fallback
                for (JsonNode match : matches) {
                    if (match.get("1. symbol").asText().equalsIgnoreCase(ticker)) {
                        companyName = match.get("2. name").asText();
                        break;
                    }
                }
            }
        }

        stockRepository.save(new Stock(ticker, companyName));
        System.out.println("Aktie hinzugefügt: " + ticker + " — " + companyName);
    }

    /**
     * Vollständige Preishistorie inkl. letzter 7 Tage — für die Reveal-Seite nach Auflösung.
     */
    @Transactional(readOnly = true)
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

    public String generateCodename(UUID stockId) {
        String[] adjectives = {"Silent", "Golden", "Iron", "Silver", "Phantom",
                "Neon", "Cosmic", "Electric", "Turbo", "Apex"};
        String[] nouns = {"Falcon", "Titan", "Nova", "Orbit", "Pulse",
                "Vortex", "Comet", "Nexus", "Spark", "Zenith"};
        int hash = Math.abs(stockId.hashCode());
        return adjectives[hash % adjectives.length] + " " + nouns[(hash / 10) % nouns.length];
    }

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