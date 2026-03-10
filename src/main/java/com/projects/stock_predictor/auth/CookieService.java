package com.projects.stock_predictor.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    @Value("${jwt.expiry.ms}")
    private long expiryMs;

    /**
     * Sets a secure HttpOnly JWT cookie on the response.
     * HttpOnly = JavaScript cannot read it (XSS protection).
     * SameSite=Strict = not sent on cross-site requests (CSRF protection).
     */
    public void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production (requires HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge((int) (expiryMs / 1000));
        response.addCookie(cookie);

        // Also set SameSite via header (Cookie API doesn't support it directly)
        response.addHeader("Set-Cookie",
                String.format("jwt=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Strict",
                        token, (int) (expiryMs / 1000)));
    }

    /**
     * Clears the JWT cookie (used on logout).
     */
    public void clearJwtCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                "jwt=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict");
    }
}