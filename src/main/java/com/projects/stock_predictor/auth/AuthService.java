package com.projects.stock_predictor.auth;

import com.projects.stock_predictor.config.JwtService;
import com.projects.stock_predictor.user.User;
import com.projects.stock_predictor.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user and returns a JWT token — same shape as login().
     */
    public TokenAndUser register(AuthRequests.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new TokenAndUser(token,
                new AuthRequests.AuthResponse(
                        user.getUsername(),
                        user.getEmail(),
                        user.getId().toString(),
                        token
                ));
    }

    /**
     * Authenticates a user and returns their info + a JWT token.
     */
    public TokenAndUser login(AuthRequests.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new TokenAndUser(token,
                new AuthRequests.AuthResponse(
                        user.getUsername(),
                        user.getEmail(),
                        user.getId().toString(),
                        token
                ));
    }

    public AuthRequests.AuthResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new AuthRequests.AuthResponse(
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                null  // no token needed for /me
        );
    }

    public record TokenAndUser(String token, AuthRequests.AuthResponse user) {}
}
