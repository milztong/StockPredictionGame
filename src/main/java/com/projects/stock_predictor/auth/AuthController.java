package com.projects.stock_predictor.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * These endpoints are no longer active.
 *
 * As of Stufe 3, StockPredictor is a microservice in the PulseStack ecosystem.
 * Authentication is handled by PulseStack's auth-service.
 *
 * Register / Login:  POST http://localhost:8084/api/v1/auth/register
 *                    POST http://localhost:8084/api/v1/auth/login
 *
 * The returned JWT works for both PulseStack and StockPredictor (shared secret).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Map<String, String> MOVED_RESPONSE = Map.of(
            "message", "Auth has moved to PulseStack auth-service.",
            "register", "POST http://localhost:8084/api/v1/auth/register",
            "login",    "POST http://localhost:8084/api/v1/auth/login"
    );

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register() {
        return ResponseEntity.status(HttpStatus.GONE).body(MOVED_RESPONSE);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        return ResponseEntity.status(HttpStatus.GONE).body(MOVED_RESPONSE);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build(); // stateless — nothing to do
    }
}
