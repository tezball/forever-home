package com.example.foreverhome.config;

import com.example.foreverhome.service.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private MetricsService metricsService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(metricsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("should not filter GET requests")
        void shouldNotFilterGetRequests() {
            request.setMethod("GET");
            request.setRequestURI("/api/auth/verify");

            boolean result = rateLimitFilter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should not filter non-auth endpoints")
        void shouldNotFilterNonAuthEndpoints() {
            request.setMethod("POST");
            request.setRequestURI("/api/pets");

            boolean result = rateLimitFilter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should filter POST requests to auth endpoints")
        void shouldFilterPostRequestsToAuthEndpoints() {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");

            boolean result = rateLimitFilter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Login Rate Limiting")
    class LoginRateLimiting {

        @Test
        @DisplayName("should allow login requests within limit")
        void shouldAllowLoginRequestsWithinLimit() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.1");

            // Make 10 requests (the limit)
            for (int i = 0; i < 10; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
                assertThat(freshResponse.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            }

            verify(filterChain, times(10)).doFilter(any(), any());
        }

        @Test
        @DisplayName("should block login requests exceeding limit")
        void shouldBlockLoginRequestsExceedingLimit() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.2");

            // Make 10 requests (the limit)
            for (int i = 0; i < 10; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
            }

            // 11th request should be blocked
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, blockedResponse, filterChain);

            assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(blockedResponse.getContentAsString()).contains("Too many requests");
        }
    }

    @Nested
    @DisplayName("Register Rate Limiting")
    class RegisterRateLimiting {

        @Test
        @DisplayName("should allow register requests within limit")
        void shouldAllowRegisterRequestsWithinLimit() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/register");
            request.setRemoteAddr("192.168.1.3");

            // Make 5 requests (the limit)
            for (int i = 0; i < 5; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
                assertThat(freshResponse.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            }

            verify(filterChain, times(5)).doFilter(any(), any());
        }

        @Test
        @DisplayName("should block register requests exceeding limit")
        void shouldBlockRegisterRequestsExceedingLimit() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/register");
            request.setRemoteAddr("192.168.1.4");

            // Make 5 requests (the limit)
            for (int i = 0; i < 5; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
            }

            // 6th request should be blocked
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, blockedResponse, filterChain);

            assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    @Nested
    @DisplayName("Password Reset Rate Limiting")
    class PasswordResetRateLimiting {

        @Test
        @DisplayName("should allow password reset requests within limit")
        void shouldAllowPasswordResetRequestsWithinLimit() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/forgot-password");
            request.setRemoteAddr("192.168.1.5");

            // Make 3 requests (the limit)
            for (int i = 0; i < 3; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
                assertThat(freshResponse.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            }

            verify(filterChain, times(3)).doFilter(any(), any());
        }

        @Test
        @DisplayName("should block password reset requests exceeding limit")
        void shouldBlockPasswordResetRequestsExceedingLimit() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/forgot-password");
            request.setRemoteAddr("192.168.1.6");

            // Make 3 requests (the limit)
            for (int i = 0; i < 3; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
            }

            // 4th request should be blocked
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, blockedResponse, filterChain);

            assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("should also limit reset-password endpoint")
        void shouldAlsoLimitResetPasswordEndpoint() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/reset-password");
            request.setRemoteAddr("192.168.1.7");

            // Make 3 requests (the limit)
            for (int i = 0; i < 3; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
            }

            // 4th request should be blocked
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, blockedResponse, filterChain);

            assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    @Nested
    @DisplayName("IP Address Handling")
    class IpAddressHandling {

        @Test
        @DisplayName("should use X-Forwarded-For header when present")
        void shouldUseXForwardedForHeaderWhenPresent() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("10.0.0.1"); // Proxy IP
            request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1"); // Real IP + proxy

            // Make requests from "real" IP
            for (int i = 0; i < 10; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
            }

            // 11th request should be blocked
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, blockedResponse, filterChain);

            assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("should separate rate limits by IP")
        void shouldSeparateRateLimitsByIp() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");

            // Exhaust limit for IP 1
            request.setRemoteAddr("192.168.1.100");
            for (int i = 0; i < 10; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
            }

            // Request from IP 2 should still be allowed
            request.setRemoteAddr("192.168.1.101");
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response2, filterChain);

            assertThat(response2.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    @Nested
    @DisplayName("Non-Auth Endpoints")
    class NonAuthEndpoints {

        @Test
        @DisplayName("should pass through non-auth POST requests without rate limiting")
        void shouldPassThroughNonAuthPostRequests() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/pets");
            request.setRemoteAddr("192.168.1.8");

            // Make many requests - should all pass through
            for (int i = 0; i < 100; i++) {
                MockHttpServletResponse freshResponse = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, freshResponse, filterChain);
                assertThat(freshResponse.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            }

            verify(filterChain, times(100)).doFilter(any(), any());
        }
    }
}
