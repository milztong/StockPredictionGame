package com.projects.stock_predictor.auth;

import com.projects.stock_predictor.user.User;
import com.projects.stock_predictor.user.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Auth logic for StockPredictor.
 *
 * Register/login have moved to PulseStack's auth-service (:8084).
 * This class only handles /me lookups for already-authenticated users.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the current user's info.
     * Username comes from the JWT subject (set by PulseStack auth-service).
     */
    public AuthRequests.AuthResponse getMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return new AuthRequests.AuthResponse(
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                null  // no token returned for /me
        );
    }
}
