package com.projects.stock_predictor.prediction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmitPredictionRequest(

        @NotNull(message = "stockId is required")
        UUID stockId,

        @NotNull(message = "predictedPrice is required")
        @DecimalMin(value = "0.01", message = "predictedPrice must be greater than 0")
        BigDecimal predictedPrice,

        @NotNull(message = "direction is required")
        Prediction.Direction direction,

        @NotNull(message = "userId is required")
        UUID userId // Temporary until Phase 3 auth — will be pulled from JWT then
) {}