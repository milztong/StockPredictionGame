package com.projects.stock_predictor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(mock(JwtAuthFilter.class));
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void allowsJinodoProductionOrigin() {
        CorsConfiguration cors = configurationFor("https://jinodo.tongmilz.com");

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("https://jinodo.tongmilz.com"))
                .isEqualTo("https://jinodo.tongmilz.com");
    }

    @Test
    void rejectsUnknownOrigin() {
        CorsConfiguration cors = configurationFor("https://example.com");

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("https://example.com")).isNull();
    }

    private CorsConfiguration configurationFor(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/leaderboard");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        return securityConfig.corsConfigurationSource().getCorsConfiguration(request);
    }
}
