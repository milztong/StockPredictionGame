package com.projects.stock_predictor.result;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /**
     * GET /api/results/{predictionId}
     * Returns the resolved result — includes ticker + company name reveal.
     * Returns 500 if prediction not yet resolved.
     */
    @GetMapping("/{predictionId}")
    public ResponseEntity<ResultService.ResultResponse> getResult(@PathVariable UUID predictionId) {
        return ResponseEntity.ok(resultService.getResult(predictionId));
    }
}

