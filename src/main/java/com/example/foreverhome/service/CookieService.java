package com.example.foreverhome.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Service for managing secure httpOnly cookies for authentication tokens.
 * Tokens stored in httpOnly cookies are not accessible to JavaScript,
 * providing protection against XSS attacks.
 */
@Service
public class CookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private static final int ACCESS_TOKEN_MAX_AGE = 15 * 60; // 15 minutes
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Strict}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    /**
     * Sets the access token as an httpOnly cookie.
     */
    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_MAX_AGE);
    }

    /**
     * Sets the refresh token as an httpOnly cookie.
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_MAX_AGE);
    }

    /**
     * Sets both access and refresh token cookies.
     */
    public void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
    }

    /**
     * Clears both token cookies (for logout).
     */
    public void clearTokenCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE);
        clearCookie(response, REFRESH_TOKEN_COOKIE);
    }

    /**
     * Extracts the access token from cookies.
     */
    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * Extracts the refresh token from cookies.
     */
    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookie.setDomain(cookieDomain);
        }

        // Add SameSite attribute via header (not directly supported by Cookie class)
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly%s; SameSite=%s",
                name, value, maxAge,
                secureCookie ? "; Secure" : "",
                sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieHeader += "; Domain=" + cookieDomain;
        }

        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        String cookieHeader = String.format("%s=; Max-Age=0; Path=/; HttpOnly%s; SameSite=%s",
                name,
                secureCookie ? "; Secure" : "",
                sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieHeader += "; Domain=" + cookieDomain;
        }

        response.addHeader("Set-Cookie", cookieHeader);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
