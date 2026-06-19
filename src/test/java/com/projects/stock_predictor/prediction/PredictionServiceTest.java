package com.projects.stock_predictor.prediction;

import com.projects.stock_predictor.stock.Stock;
import com.projects.stock_predictor.stock.StockPrice;
import com.projects.stock_predictor.stock.StockPriceRepository;
import com.projects.stock_predictor.stock.StockRepository;
import com.projects.stock_predictor.stock.StockService;
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
class PredictionServiceTest {

    @Mock PredictionRepository predictionRepository;
    @Mock StockRepository stockRepository;
    @Mock StockPriceRepository stockPriceRepository;
    @Mock StockService stockService;

    @InjectMocks PredictionService predictionService;

    private UUID stockId;
    private Stock stock;

    @BeforeEach
    void setUp() {
        stockId = UUID.randomUUID();
        stock = new Stock();
        stock.setId(stockId);
        stock.setTicker("TSLA");
        stock.setCompanyName("Tesla Inc.");
    }

    @Test
    void submit_stockNotFound_throwsIllegalArgument() {
        UUID unknownId = UUID.randomUUID();
        when(stockRepository.findById(unknownId)).thenReturn(Optional.empty());

        SubmitPredictionRequest req = new SubmitPredictionRequest(
                unknownId, new BigDecimal("250.00"), Prediction.Direction.UP, UUID.randomUUID());

        assertThatThrownBy(() -> predictionService.submit(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void submit_noPriceData_throwsIllegalState() {
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stockPriceRepository.findByStockIdAndDateBetweenOrderByDateAsc(any(), any(), any()))
                .thenReturn(List.of());

        SubmitPredictionRequest req = new SubmitPredictionRequest(
                stockId, new BigDecimal("250.00"), Prediction.Direction.UP, UUID.randomUUID());

        assertThatThrownBy(() -> predictionService.submit(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(stockId.toString());
    }

    @Test
    void submit_setsTargetDateToSevenDaysFromNow() {
        UUID userId = UUID.randomUUID();
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stockPriceRepository.findByStockIdAndDateBetweenOrderByDateAsc(any(), any(), any()))
                .thenReturn(List.of(priceOf("200.00")));
        when(stockService.generateCodename(stockId)).thenReturn("Neon Pulse");

        Prediction saved = new Prediction();
        saved.setId(UUID.randomUUID());
        saved.setStock(stock);
        saved.setUserId(userId);
        saved.setPredictedPrice(new BigDecimal("250.00"));
        saved.setDirection(Prediction.Direction.UP);
        saved.setBasePrice(new BigDecimal("200.00"));
        saved.setTargetDate(LocalDate.now().plusDays(7));
        saved.setSubmittedAt(LocalDateTime.now());
        saved.setStatus(Prediction.Status.PENDING);
        saved.setStockCodename("Neon Pulse");
        when(predictionRepository.save(any())).thenReturn(saved);

        SubmitPredictionRequest req = new SubmitPredictionRequest(
                stockId, new BigDecimal("250.00"), Prediction.Direction.UP, userId);

        PredictionService.PredictionResponse response = predictionService.submit(req);

        assertThat(response.targetDate()).isEqualTo(LocalDate.now().plusDays(7).toString());
        assertThat(response.status()).isEqualTo(Prediction.Status.PENDING);
    }

    @Test
    void submit_usesLastAvailablePriceAsBasePrice() {
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        StockPrice older = priceOf("180.00");
        StockPrice newer = priceOf("200.00");
        when(stockPriceRepository.findByStockIdAndDateBetweenOrderByDateAsc(any(), any(), any()))
                .thenReturn(List.of(older, newer));
        when(stockService.generateCodename(any())).thenReturn("Iron Titan");

        Prediction saved = new Prediction();
        saved.setId(UUID.randomUUID());
        saved.setStock(stock);
        saved.setUserId(UUID.randomUUID());
        saved.setPredictedPrice(new BigDecimal("220.00"));
        saved.setDirection(Prediction.Direction.UP);
        saved.setBasePrice(new BigDecimal("200.00"));
        saved.setTargetDate(LocalDate.now().plusDays(7));
        saved.setSubmittedAt(LocalDateTime.now());
        saved.setStatus(Prediction.Status.PENDING);
        saved.setStockCodename("Iron Titan");
        when(predictionRepository.save(any())).thenReturn(saved);

        predictionService.submit(new SubmitPredictionRequest(
                stockId, new BigDecimal("220.00"), Prediction.Direction.UP, UUID.randomUUID()));

        ArgumentCaptor<Prediction> captor = ArgumentCaptor.forClass(Prediction.class);
        verify(predictionRepository).save(captor.capture());
        assertThat(captor.getValue().getBasePrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void getMyPredictions_returnsOnlyUsersPredictions() {
        UUID userId = UUID.randomUUID();
        Prediction p = new Prediction();
        p.setId(UUID.randomUUID());
        p.setUserId(userId);
        p.setStock(stock);
        p.setPredictedPrice(new BigDecimal("100.00"));
        p.setDirection(Prediction.Direction.DOWN);
        p.setBasePrice(new BigDecimal("110.00"));
        p.setTargetDate(LocalDate.now().plusDays(7));
        p.setSubmittedAt(LocalDateTime.now());
        p.setStatus(Prediction.Status.PENDING);
        p.setStockCodename("Silent Nova");

        when(predictionRepository.findByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(p));

        List<PredictionService.PredictionResponse> results = predictionService.getMyPredictions(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).direction()).isEqualTo(Prediction.Direction.DOWN);
    }

    @Test
    void getPrediction_notFound_throwsIllegalArgument() {
        UUID predId = UUID.randomUUID();
        when(predictionRepository.findById(predId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> predictionService.getPrediction(predId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(predId.toString());
    }

    private StockPrice priceOf(String close) {
        StockPrice sp = new StockPrice();
        sp.setClose(new BigDecimal(close));
        return sp;
    }
}
