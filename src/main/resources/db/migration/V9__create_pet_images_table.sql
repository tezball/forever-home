-- V9: Create pet images table
-- Photos of pets to help adopters connect emotionally

CREATE TABLE pet_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_pet_images_pet ON pet_images(pet_id);
CREATE INDEX idx_pet_images_primary ON pet_images(pet_id) WHERE is_primary = TRUE;

COMMENT ON TABLE pet_images IS 'Pet photos stored in S3';
COMMENT ON COLUMN pet_images.is_primary IS 'Main listing photo shown in search results';
COMMENT ON COLUMN pet_images.display_order IS 'Order for gallery display';
