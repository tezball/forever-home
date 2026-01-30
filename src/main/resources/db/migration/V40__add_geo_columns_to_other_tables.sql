-- V40: Add geolocation columns to fosters, adopters, and vets tables
-- These tables also embed the Address value object which now includes lat/lon

-- Add latitude and longitude columns to fosters
ALTER TABLE fosters
ADD COLUMN IF NOT EXISTS address_latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS address_longitude DOUBLE PRECISION;

-- Add latitude and longitude columns to adopters
ALTER TABLE adopters
ADD COLUMN IF NOT EXISTS address_latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS address_longitude DOUBLE PRECISION;

-- Add latitude and longitude columns to vets
ALTER TABLE vets
ADD COLUMN IF NOT EXISTS address_latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS address_longitude DOUBLE PRECISION;

COMMENT ON COLUMN fosters.address_latitude IS 'Latitude coordinate from geocoding service';
COMMENT ON COLUMN fosters.address_longitude IS 'Longitude coordinate from geocoding service';
COMMENT ON COLUMN adopters.address_latitude IS 'Latitude coordinate from geocoding service';
COMMENT ON COLUMN adopters.address_longitude IS 'Longitude coordinate from geocoding service';
COMMENT ON COLUMN vets.address_latitude IS 'Latitude coordinate from geocoding service';
COMMENT ON COLUMN vets.address_longitude IS 'Longitude coordinate from geocoding service';
