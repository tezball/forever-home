package com.example.foreverhome.security;

import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Provides JWT token generation and validation functionality.
 */
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_VERIFIED = "verified";
    private static final long REMEMBER_ME_MULTIPLIER = 4; // 30 days vs 7 days

    private final SecretKey secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenProvider(String secret, long accessTokenExpiryMs, long refreshTokenExpiryMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /**
     * Generates an access token for the given user.
     *
     * @param user the user
     * @return the JWT access token
     */
    public String generateAccessToken(User user) {
        // VET and RESCUE_ORG require verification, assume verified if profile is complete
        boolean verified = !user.requiresVerification() || user.isProfileComplete();
        return generateAccessToken(user.getId(), user.getRole(), verified);
    }

    /**
     * Generates an access token for the given user.
     *
     * @param userId   the user's UUID
     * @param role     the user's role
     * @param verified whether the user is verified (for VET and RESCUE_ORG)
     * @return the JWT access token
     */
    public String generateAccessToken(UUID userId, UserRole role, boolean verified) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiryMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_VERIFIED, verified)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a refresh token for the given user.
     *
     * @param userId the user's UUID
     * @return the JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        return generateRefreshToken(userId, false);
    }

    /**
     * Generates a refresh token for the given user.
     *
     * @param userId     the user's UUID
     * @param rememberMe if true, extends token expiry to 30 days
     * @return the JWT refresh token
     */
    public String generateRefreshToken(UUID userId, boolean rememberMe) {
        Date now = new Date();
        long expiry = rememberMe ? refreshTokenExpiryMs * REMEMBER_ME_MULTIPLIER : refreshTokenExpiryMs;
        Date expiryDate = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the user ID from a token.
     *
     * @param token the JWT token
     * @return the user's UUID
     * @throws InvalidTokenException if the token is invalid
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the user role from a token.
     *
     * @param token the JWT token
     * @return the user's role
     * @throws InvalidTokenException if the token is invalid
     */
    public UserRole getRoleFromToken(String token) {
        Claims claims = parseClaimsFromToken(token);
        String role = claims.get(CLAIM_ROLE, String.class);
        return UserRole.valueOf(role);
    }

    /**
     * Extracts the verified status from a token.
     *
     * @param token the JWT token
     * @return true if the user is verified
     * @throws InvalidTokenException if the token is invalid
     */
    public boolean isVerifiedFromToken(String token) {
        Claims claims = parseClaimsFromToken(token);
        return claims.get(CLAIM_VERIFIED, Boolean.class);
    }

    /**
     * Validates a token.
     *
     * @param token the JWT token
     * @return true if the token is valid
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            logger.warn("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.warn("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }

    private Claims parseClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }
}
