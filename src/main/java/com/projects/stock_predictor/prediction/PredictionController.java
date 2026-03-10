package com.projects.stock_predictor.prediction;

import com.projects.stock_predictor.config.JwtService;
import com.projects.stock_predictor.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public PredictionController(PredictionService predictionService,
                                UserRepository userRepository,
                                JwtService jwtService) {
        this.predictionService = predictionService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/predictions/submit
     * userId is now extracted from the JWT cookie — not sent by the client.
     */
    @PostMapping("/submit")
    public ResponseEntity<PredictionService.PredictionResponse> submit(
            @Valid @RequestBody SubmitPredictionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = resolveUserId(userDetails);
        SubmitPredictionRequest authenticatedRequest = new SubmitPredictionRequest(
                request.stockId(),
                request.predictedPrice(),
                request.direction(),
                userId // overwrite any userId from request body with the real one from JWT
        );
        return ResponseEntity.ok(predictionService.submit(authenticatedRequest));
    }

    /**
     * GET /api/predictions/my
     * Returns predictions for the currently logged-in user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<PredictionService.PredictionResponse>> getMyPredictions(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(predictionService.getMyPredictions(userId));
    }

    /**
     * GET /api/predictions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PredictionService.PredictionResponse> getPrediction(
            @PathVariable UUID id) {
        return ResponseEntity.ok(predictionService.getPrediction(id));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"))
                .getId();
    }
}
