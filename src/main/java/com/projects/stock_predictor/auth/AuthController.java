package com.projects.stock_predictor.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Creates a new user and returns JWT token in response body.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthRequests.AuthResponse> register(
            @Valid @RequestBody AuthRequests.RegisterRequest request) {
        AuthService.TokenAndUser result = authService.register(request);
        return ResponseEntity.ok(new AuthRequests.AuthResponse(
                result.user().username(),
                result.user().email(),
                result.user().userId(),
                result.token()
        ));
    }

    /**
     * POST /api/auth/login
     * Validates credentials and returns JWT token in response body.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthRequests.AuthResponse> login(
            @Valid @RequestBody AuthRequests.LoginRequest request) {
        AuthService.TokenAndUser result = authService.login(request);
        return ResponseEntity.ok(new AuthRequests.AuthResponse(
                result.user().username(),
                result.user().email(),
                result.user().userId(),
                result.token()
        ));
    }

    /**
     * POST /api/auth/logout
     * Stateless — client just drops the token. Nothing to clear server-side.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/auth/me
     * Returns the currently authenticated user's info.
     * Spring Security populates @AuthenticationPrincipal from the Authorization header.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthRequests.AuthResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getMe(userDetails.getUsername()));
    }
}