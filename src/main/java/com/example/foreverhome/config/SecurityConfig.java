package com.example.foreverhome.config;

import com.example.foreverhome.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF protection is disabled for stateless JWT authentication
            // JWT tokens are stored in localStorage and sent via Authorization header
            // Cross-origin requests cannot read/set custom headers, providing CSRF protection
            // TODO: Enable CSRF when migrating tokens to httpOnly cookies
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Security headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://accounts.google.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data: https: http://localhost:* blob:; " +
                        "connect-src 'self' https://accounts.google.com; " +
                        "frame-src https://accounts.google.com; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self'; " +
                        "base-uri 'self'"))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> {})
                .referrerPolicy(referrer -> referrer
                    .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions
                    .policy("geolocation=(), microphone=(), camera=()"))
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/dev/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pets").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pets/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pets/{id}/history").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rescues").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rescues/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rescues/{id}/pets").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vets").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stats").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pets/featured").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()

                // Static resources for SPA
                .requestMatchers("/", "/index.html").permitAll()
                .requestMatchers("/assets/**").permitAll()
                .requestMatchers("/*.js", "/*.css", "/*.ico", "/*.svg", "/*.png", "/*.jpg").permitAll()

                // SPA frontend routes - allow all non-API routes for client-side routing
                // These are served by SpaWebConfig which returns index.html
                // Note: Admin features have been moved to moderation-service (port 8081)
                .requestMatchers(
                    "/login", "/register", "/register/google", "/forgot-password", "/reset-password", "/verify-email/**",
                    "/pets", "/pets/**",
                    "/swipe",  // Redirect to /pets/swipe
                    "/rescues", "/rescues/**",
                    "/vets", "/vets/**",
                    "/faq", "/contact", "/privacy", "/help", "/about", "/terms",
                    "/profile", "/settings", "/notifications",
                    "/adopter/**", "/foster/**", "/rescue/**", "/vet/**",
                    "/admin", "/admin/**"  // Super admin dashboard routes
                ).permitAll()

                // Authenticated endpoints
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOriginsList());
        configuration.setAllowedMethods(corsProperties.getAllowedMethodsList());
        // Explicitly list allowed headers instead of wildcard for security
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Cache-Control"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
