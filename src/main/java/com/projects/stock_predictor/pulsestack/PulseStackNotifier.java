package com.projects.stock_predictor.pulsestack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sendet Events an den PulseStack External-Ingest-Endpoint.
 * Nur aktiv wenn pulsestack.ingest.url konfiguriert ist.
 *
 * Fehler werden geloggt aber nie geworfen — PulseStack ist ein optionales
 * Enhancement, kein kritischer Pfad des StockPredictors.
 */
@Service
@ConditionalOnProperty(name = "pulsestack.ingest.url")
public class PulseStackNotifier {

    private static final Logger log = LoggerFactory.getLogger(PulseStackNotifier.class);
    private static final String INGEST_PATH = "/api/v1/ingest/external";

    private final RestClient restClient;
    private final String ingestSecret;

    public PulseStackNotifier(
            @Value("${pulsestack.ingest.url}") String ingestUrl,
            @Value("${pulsestack.ingest.secret:change-me-in-production}") String ingestSecret
    ) {
        this.restClient  = RestClient.builder().baseUrl(ingestUrl).build();
        this.ingestSecret = ingestSecret;
    }

    /**
     * Sendet ein Event asynchron — blockiert den aufrufenden Thread nicht.
     */
    public void send(PulseStackEvent event) {
        Thread.ofVirtual().start(() -> doSend(event));
    }

    private void doSend(PulseStackEvent event) {
        try {
            restClient.post()
                    .uri(INGEST_PATH)
                    .header("X-Ingest-Secret", ingestSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();

            log.info("PulseStack notified: [{}] {}", event.channelName(), event.title());
        } catch (Exception e) {
            // Nie den StockPredictor-Flow unterbrechen
            log.warn("Could not notify PulseStack (is it running?): {}", e.getMessage());
        }
    }
}
