package com.projects.stock_predictor.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** Null for SSO users created automatically from PulseStack JWT. */
    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "total_score", nullable = false)
    private int totalScore = 0;

    /** Legacy constructor — used when StockPredictor had its own auth. */
    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** SSO constructor — user is created automatically from a PulseStack JWT. */
    public User(String username) {
        this.username = username;
        this.email = username + "@pulsestack.sso";  // synthetic, never used for login
        this.passwordHash = "{noop}__sso__";  // placeholder — SSO users never log in with password
    }
}
