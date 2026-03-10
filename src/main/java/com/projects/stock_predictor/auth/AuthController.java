package com.projects.stock_predictor.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    /**
     * POST /api/auth/register
     * Creates a new user. Does NOT log them in automatically — they must call /login next.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthRequests.AuthResponse> register(
            @Valid @RequestBody AuthRequests.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Validates credentials and sets a JWT HttpOnly cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthRequests.AuthResponse> login(
            @Valid @RequestBody AuthRequests.LoginRequest request,
            HttpServletResponse response) {
        AuthService.TokenAndUser result = authService.login(request);
        cookieService.setJwtCookie(response, result.token());
        return ResponseEntity.ok(result.user());
    }

    /**
     * POST /api/auth/logout
     * Clears the JWT cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        cookieService.clearJwtCookie(response);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/auth/me
     * Returns the currently authenticated user's info.
     * Spring Security populates @AuthenticationPrincipal from the JWT cookie automatically.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthRequests.AuthResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getMe(userDetails.getUsername()));
    }
}