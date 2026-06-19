package com.projects.stock_predictor.stock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StockServiceCodenameTest {

    @Mock StockRepository stockRepository;
    @Mock StockPriceRepository stockPriceRepository;
    @Mock com.projects.stock_predictor.challenge.DailyChallengeRepository dailyChallengeRepository;
    @Mock okhttp3.OkHttpClient httpClient;

    @InjectMocks StockService stockService;

    @Test
    void generateCodename_isTwoWords() {
        String codename = stockService.generateCodename(UUID.randomUUID());
        assertThat(codename.split(" ")).hasSize(2);
    }

    @Test
    void generateCodename_isDeterministic() {
        UUID id = UUID.randomUUID();
        assertThat(stockService.generateCodename(id)).isEqualTo(stockService.generateCodename(id));
    }

    @Test
    void generateCodename_differentIdsProduceDifferentNames() {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            names.add(stockService.generateCodename(UUID.randomUUID()));
        }
        // With 10×10=100 possible names, 20 random IDs should produce at least 2 distinct names
        assertThat(names.size()).isGreaterThan(1);
    }

    @Test
    void generateCodename_usesKnownAdjectives() {
        Set<String> knownAdjectives = Set.of(
                "Silent", "Golden", "Iron", "Silver", "Phantom",
                "Neon", "Cosmic", "Electric", "Turbo", "Apex"
        );
        for (int i = 0; i < 30; i++) {
            String codename = stockService.generateCodename(UUID.randomUUID());
            String adjective = codename.split(" ")[0];
            assertThat(knownAdjectives).contains(adjective);
        }
    }
}
