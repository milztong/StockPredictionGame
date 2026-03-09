package com.projects.stock_predictor.prediction;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    /**
     * POST /api/predictions/submit
     * Body: { stockId, predictedPrice, direction, userId }
     */
    @PostMapping("/submit")
    public ResponseEntity<PredictionService.PredictionResponse> submit(
            @Valid @RequestBody SubmitPredictionRequest request) {
        return ResponseEntity.ok(predictionService.submit(request));
    }

    /**
     * GET /api/predictions/my?userId={uuid}
     * Returns all predictions for a user, newest first.
     * userId will be removed in Phase 3 — pulled from JWT cookie instead.
     */
    @GetMapping("/my")
    public ResponseEntity<List<PredictionService.PredictionResponse>> getMyPredictions(
            @RequestParam UUID userId) {
        return ResponseEntity.ok(predictionService.getMyPredictions(userId));
    }

    /**
     * GET /api/predictions/{id}
     * Returns a single prediction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PredictionService.PredictionResponse> getPrediction(
            @PathVariable UUID id) {
        return ResponseEntity.ok(predictionService.getPrediction(id));
    }
}
