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
public class AlphaVantageClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.api.key}")
    private String apiKey;

    public AlphaVantageClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches daily OHLCV data for a given ticker from Alpha Vantage.
     * Returns a list of raw price data (not yet linked to a Stock entity).
     */
    public List<PriceData> fetchDailyPrices(String ticker) throws IOException {
        String url = "https://www.alphavantage.co/query"
                + "?function=TIME_SERIES_DAILY"
                + "&symbol=" + ticker
                + "&outputsize=compact" // last 100 trading days — enough for our 90-day window
                + "&apikey=" + apiKey;

        Request request = new Request.Builder().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Alpha Vantage request failed: " + response.code());
            }

            String json = response.body().string();
            return parseResponse(json, ticker);
        }
    }

    private List<PriceData> parseResponse(String json, String ticker) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        // Alpha Vantage returns an error message node if something went wrong
        if (root.has("Note") || root.has("Information")) {
            throw new IOException("Alpha Vantage API limit reached or invalid key for ticker: " + ticker);
        }

        JsonNode timeSeries = root.get("Time Series (Daily)");
        if (timeSeries == null) {
            throw new IOException("Unexpected Alpha Vantage response format for ticker: " + ticker);
        }

        List<PriceData> prices = new ArrayList<>();
        timeSeries.fields().forEachRemaining(entry -> {
            LocalDate date = LocalDate.parse(entry.getKey());
            JsonNode day = entry.getValue();
            prices.add(new PriceData(
                    date,
                    new BigDecimal(day.get("1. open").asText()),
                    new BigDecimal(day.get("2. high").asText()),
                    new BigDecimal(day.get("3. low").asText()),
                    new BigDecimal(day.get("4. close").asText()),
                    Long.parseLong(day.get("5. volume").asText())
            ));
        });

        return prices;
    }

    // Simple record to hold raw parsed data before saving to DB
    public record PriceData(
            LocalDate date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            Long volume
    ) {}
}
