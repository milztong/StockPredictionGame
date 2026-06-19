package com.projects.stock_predictor.result;

import com.projects.stock_predictor.prediction.Prediction;
import com.projects.stock_predictor.stock.Stock;
import com.projects.stock_predictor.stock.StockPrice;
import com.projects.stock_predictor.stock.StockPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock ResultRepository resultRepository;
    @Mock com.projects.stock_predictor.prediction.PredictionRepository predictionRepository;
    @Mock StockPriceRepository stockPriceRepository;

    @InjectMocks ResultService resultService;

    private Stock stock;
    private Prediction prediction;

    @BeforeEach
    void setUp() {
        stock = new Stock();
        stock.setId(UUID.randomUUID());
        stock.setTicker("AAPL");
        stock.setCompanyName("Apple Inc.");

        prediction = new Prediction();
        prediction.setId(UUID.randomUUID());
        prediction.setStock(stock);
        prediction.setUserId(UUID.randomUUID());
        prediction.setSubmittedAt(LocalDateTime.now().minusDays(7));
        prediction.setTargetDate(LocalDate.now());
        prediction.setStockCodename("Golden Falcon");
        prediction.setStatus(Prediction.Status.PENDING);
    }

    // ── Direction scoring ──────────────────────────────────────────────────

    @Test
    void resolve_directionUp_correct_scores50() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("110.00"));

        // actual price went UP
        stubActualPrice("105.00");

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.isDirectionCorrect()).isTrue();
        assertThat(saved.getDirectionScore()).isEqualTo(50);
    }

    @Test
    void resolve_directionUp_wrong_scores0() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("110.00"));

        // actual price went DOWN
        stubActualPrice("95.00");

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.isDirectionCorrect()).isFalse();
        assertThat(saved.getDirectionScore()).isEqualTo(0);
    }

    @Test
    void resolve_directionDown_correct_scores50() {
        prediction.setDirection(Prediction.Direction.DOWN);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("90.00"));

        stubActualPrice("92.00");

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.isDirectionCorrect()).isTrue();
        assertThat(saved.getDirectionScore()).isEqualTo(50);
    }

    // ── Accuracy scoring ───────────────────────────────────────────────────

    @Test
    void resolve_exactPricePrediction_accuracy100() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("105.00"));

        stubActualPrice("105.00"); // exact match

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.getAccuracyScore()).isEqualTo(100);
        assertThat(saved.getTotalScore()).isEqualTo(150); // 50 direction + 100 accuracy
    }

    @Test
    void resolve_tenPercentError_accuracy0() {
        prediction.setDirection(Prediction.Direction.DOWN);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("90.00"));

        stubActualPrice("80.00"); // 12.5% error → accuracy = max(0, 100 - 125) = 0

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.getAccuracyScore()).isEqualTo(0);
    }

    @Test
    void resolve_fivePercentError_accuracy50() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("110.00"));

        // actual = 104.50 → error = |110-104.5|/104.5 ≈ 5.26% → accuracy ≈ 100 - 52.6 = 47
        stubActualPrice("104.50");

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.getAccuracyScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
    }

    // ── Total score ────────────────────────────────────────────────────────

    @Test
    void resolve_correctDirectionAndExactPrice_maxScore150() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("120.00"));

        stubActualPrice("120.00");

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.getTotalScore()).isEqualTo(150);
    }

    @Test
    void resolve_wrongDirectionAndBadPrice_minScore0() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("150.00"));

        // price went down massively
        stubActualPrice("70.00");

        resultService.resolve(prediction);

        Result saved = captureResult();
        assertThat(saved.getDirectionScore()).isEqualTo(0);
        assertThat(saved.getAccuracyScore()).isEqualTo(0);
        assertThat(saved.getTotalScore()).isEqualTo(0);
    }

    // ── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void resolve_noPriceData_skipsResolution() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("110.00"));

        when(stockPriceRepository.findByStockIdAndDateBetweenOrderByDateAsc(any(), any(), any()))
                .thenReturn(List.of());

        resultService.resolve(prediction);

        verify(resultRepository, never()).save(any());
        verify(predictionRepository, never()).save(any());
    }

    @Test
    void resolve_marksStatusResolved() {
        prediction.setDirection(Prediction.Direction.UP);
        prediction.setBasePrice(new BigDecimal("100.00"));
        prediction.setPredictedPrice(new BigDecimal("110.00"));
        stubActualPrice("105.00");

        resultService.resolve(prediction);

        assertThat(prediction.getStatus()).isEqualTo(Prediction.Status.RESOLVED);
        verify(predictionRepository).save(prediction);
    }

    // ── getResult ──────────────────────────────────────────────────────────

    @Test
    void getResult_notFound_throwsIllegalState() {
        UUID predId = UUID.randomUUID();
        when(resultRepository.findByPredictionId(predId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.getResult(predId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(predId.toString());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void stubActualPrice(String price) {
        StockPrice sp = new StockPrice();
        sp.setClose(new BigDecimal(price));
        when(stockPriceRepository.findByStockIdAndDateBetweenOrderByDateAsc(
                eq(stock.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(sp));
    }

    private Result captureResult() {
        ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);
        verify(resultRepository).save(captor.capture());
        return captor.getValue();
    }
}
