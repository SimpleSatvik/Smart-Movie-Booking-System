package com.moviebooking.config;

import com.moviebooking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    // 256-bit secret key used to digitally sign our tokens so they cannot be forged
    // In production, this can be passed via environment variable
    private static final String DEFAULT_SECRET = "SmartMovieBookingSystemSuperSecretSigningKeyForJwt2026Secure256Bit!";

    @Value("${jwt.secret:" + DEFAULT_SECRET + "}")
    private String secretKey;

    // Token validity: 24 hours in milliseconds (24 * 60 * 60 * 1000)
    private static final long EXPIRATION_TIME_MS = 86_400_000L;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generates a signed JWT token containing the user's ID, email, and role.
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole());
        extraClaims.put("name", user.getName());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getGmail()) // Email is the primary subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the user's email from the token.
     */
    public String extractGmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user's role (ROLE_USER, ROLE_THEATRE_OWNER, or ROLE_ADMIN) from the token.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Checks if the token is valid and not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
