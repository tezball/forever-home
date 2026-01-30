-- V38: Add geolocation columns to rescue organizations for proximity search
-- Supports finding rescues near a given location using Haversine formula

-- Add latitude and longitude columns
ALTER TABLE rescue_organizations
ADD COLUMN address_latitude DOUBLE PRECISION,
ADD COLUMN address_longitude DOUBLE PRECISION;

-- Create composite index for bounding box queries
-- This enables efficient pre-filtering before Haversine calculation
CREATE INDEX idx_rescue_org_coordinates
ON rescue_organizations (address_latitude, address_longitude)
WHERE address_latitude IS NOT NULL AND address_longitude IS NOT NULL;

COMMENT ON COLUMN rescue_organizations.address_latitude IS 'Latitude coordinate from geocoding service';
COMMENT ON COLUMN rescue_organizations.address_longitude IS 'Longitude coordinate from geocoding service';
