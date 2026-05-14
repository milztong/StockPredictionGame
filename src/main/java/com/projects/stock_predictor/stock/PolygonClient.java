package com.projects.stock_predictor.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PolygonClient {

    private static final int HISTORY_DAYS = 90;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${polygon.api.key}")
    private String apiKey;

    public PolygonClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public List<PriceData> fetchDailyPrices(String ticker) throws IOException {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(HISTORY_DAYS);

        String url = "https://api.polygon.io/v2/aggs/ticker/"
                + ticker
                + "/range/1/day/"
                + from
                + "/"
                + to
                + "?adjusted=true&sort=asc&limit=120&apiKey="
                + apiKey;

        Request request = new Request.Builder().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Polygon.io request failed: HTTP " + response.code()
                        + " for ticker: " + ticker);
            }

            String json = response.body().string();
            return parseResponse(json, ticker);
        }
    }

    private List<PriceData> parseResponse(String json, String ticker) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        // Polygon gibt status "OK" oder "ERROR" zurück
        String status = root.path("status").asText();
        if ("ERROR".equals(status)) {
            String error = root.path("error").asText("Unknown error");
            throw new IOException("Polygon.io error for ticker " + ticker + ": " + error);
        }

        // "resultsCount" = 0 bedeutet keine Daten (z.B. ungültiger Ticker)
        int count = root.path("resultsCount").asInt(0);
        if (count == 0) {
            throw new IOException("Polygon.io returned no data for ticker: " + ticker);
        }

        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) {
            throw new IOException("Unexpected Polygon.io response format for ticker: " + ticker);
        }

        List<PriceData> prices = new ArrayList<>();
        for (JsonNode bar : results) {
            // Polygon gibt Timestamps in Millisekunden zurück (Unix epoch)
            long timestampMs = bar.get("t").asLong();
            LocalDate date = LocalDate.ofEpochDay(timestampMs / 86_400_000L);

            prices.add(new PolygonClient.PriceData(
                    date,
                    new BigDecimal(bar.get("o").asText()), // open
                    new BigDecimal(bar.get("h").asText()), // high
                    new BigDecimal(bar.get("l").asText()), // low
                    new BigDecimal(bar.get("c").asText()), // close
                    bar.get("v").asLong()                  // volume
            ));
        }

        return prices;
    }

    public record PriceData(
            java.time.LocalDate date,
            java.math.BigDecimal open,
            java.math.BigDecimal high,
            java.math.BigDecimal low,
            java.math.BigDecimal close,
            Long volume
    ) {}
}