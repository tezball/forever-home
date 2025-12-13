-- V22: Migrate pet_images from full URLs to S3 keys
-- This enables environment-independent image storage

-- Add s3_key column
ALTER TABLE pet_images ADD COLUMN s3_key VARCHAR(500);

-- Migrate existing URLs to S3 keys
-- Handle LocalStack format: http://localhost:4566/forever-home-images/pets/xxx/yyy.jpg
-- Handle AWS format: https://forever-home-images.s3.amazonaws.com/pets/xxx/yyy.jpg
UPDATE pet_images
SET s3_key = CASE
    -- LocalStack format: extract everything after bucket name
    WHEN url LIKE '%/forever-home-images/%' THEN
        SUBSTRING(url FROM POSITION('/forever-home-images/' IN url) + LENGTH('/forever-home-images/'))
    -- AWS S3 format: extract everything after .s3.amazonaws.com/
    WHEN url LIKE '%.s3.amazonaws.com/%' THEN
        SUBSTRING(url FROM POSITION('.s3.amazonaws.com/' IN url) + LENGTH('.s3.amazonaws.com/'))
    -- Fallback: use the URL as-is (shouldn't happen, but safe default)
    ELSE url
END
WHERE s3_key IS NULL;

-- Make s3_key NOT NULL now that data is migrated
ALTER TABLE pet_images ALTER COLUMN s3_key SET NOT NULL;

-- Drop the old url column - we no longer need it
ALTER TABLE pet_images DROP COLUMN url;

-- Add comment for clarity
COMMENT ON COLUMN pet_images.s3_key IS 'S3 object key (path within bucket), URLs constructed at runtime';
