-- V39: Create geocode cache table for storing geocoding results
-- Reduces API calls to external geocoding services

CREATE TABLE geocode_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cache_key VARCHAR(500) NOT NULL UNIQUE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (NOW() + INTERVAL '90 days')
);

-- Index for cache lookups
CREATE INDEX idx_geocode_cache_key ON geocode_cache(cache_key);

-- Index for cleanup of expired entries
CREATE INDEX idx_geocode_cache_expires ON geocode_cache(expires_at);

COMMENT ON TABLE geocode_cache IS 'Cache for geocoding results to reduce API calls';
COMMENT ON COLUMN geocode_cache.cache_key IS 'Normalized address string used as lookup key';
COMMENT ON COLUMN geocode_cache.expires_at IS 'Entries older than this will be refreshed';
