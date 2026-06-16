package com.projects.stock_predictor.pulsestack;

/**
 * Payload für den PulseStack External-Ingest-Endpoint.
 * Entspricht ExternalIngestRequest in pulsestack/ingestion-service.
 */
public record PulseStackEvent(
        String channelName,
        String externalId,
        String title,
        String url,
        String thumbnailUrl,
        String author,
        Integer score
) {
    /** Erstellt ein Event ohne optionale Felder. */
    public static PulseStackEvent of(String channelName, String externalId,
                                     String title, String url, String author, Integer score) {
        return new PulseStackEvent(channelName, externalId, title, url, null, author, score);
    }
}
