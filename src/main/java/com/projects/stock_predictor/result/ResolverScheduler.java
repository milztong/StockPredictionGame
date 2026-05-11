package com.projects.stock_predictor.result;

import com.projects.stock_predictor.prediction.Prediction;
import com.projects.stock_predictor.prediction.PredictionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Resolver-Logik wurde in DailyScheduler konsolidiert.
 * Diese Klasse bleibt leer, damit bestehende Beans nicht brechen.
 * Der DailyScheduler übernimmt jetzt: Challenge-Setup + Preis-Fetch + Predictions auflösen.
 */
@Component
public class ResolverScheduler {

}