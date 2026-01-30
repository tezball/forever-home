package com.example.foreverhome.domain.geo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Cached geocoding result to reduce API calls to external services.
 */
@Table("geocode_cache")
public class GeocodeCache {

    @Id
    private UUID id;

    @Column("cache_key")
    private String cacheKey;

    @Column("latitude")
    private double latitude;

    @Column("longitude")
    private double longitude;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    protected GeocodeCache() {
    }

    public GeocodeCache(String cacheKey, double latitude, double longitude) {
        this.id = UUID.randomUUID();
        this.cacheKey = cacheKey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(90L * 24 * 60 * 60); // 90 days
    }

    public UUID getId() {
        return id;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public Coordinates toCoordinates() {
        return new Coordinates(latitude, longitude);
    }
}
