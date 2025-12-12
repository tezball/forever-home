-- V21: Verify test rescue organization
-- The test rescue org was created without verified=true, so it doesn't appear in public listings
-- This migration fixes existing data

UPDATE rescue_organizations
SET verified = true
WHERE name = 'Test Rescue Organization';
