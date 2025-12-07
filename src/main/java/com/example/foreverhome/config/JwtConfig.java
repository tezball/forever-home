package com.example.foreverhome.config;

import com.example.foreverhome.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationWhichMustBeLongEnough256Bits}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry-ms:900000}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret, accessTokenExpiryMs, refreshTokenExpiryMs);
    }
}
