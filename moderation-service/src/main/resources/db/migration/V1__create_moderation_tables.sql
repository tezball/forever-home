-- Moderation results for pet profiles
CREATE TABLE moderation_results (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    content_identifier VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    confidence_score DECIMAL(5,4),
    model_used VARCHAR(100) NOT NULL,
    raw_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID,
    review_notes TEXT,
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'APPROVED', 'FLAGGED', 'REJECTED')),
    CONSTRAINT valid_content_type CHECK (content_type IN ('TEXT', 'IMAGE'))
);

-- Flagged content details (multiple flags per moderation result)
CREATE TABLE flagged_content (
    id UUID PRIMARY KEY,
    moderation_result_id UUID NOT NULL REFERENCES moderation_results(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    suggested_action VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_category CHECK (category IN ('INAPPROPRIATE', 'MISLEADING', 'SPAM', 'SCAM', 'HARMFUL', 'NOT_PET', 'NSFW', 'UNCLEAR', 'OTHER')),
    CONSTRAINT valid_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- Track moderation jobs for batch processing
CREATE TABLE moderation_jobs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_pets INTEGER NOT NULL DEFAULT 0,
    processed_pets INTEGER NOT NULL DEFAULT 0,
    flagged_pets INTEGER NOT NULL DEFAULT 0,
    text_only BOOLEAN NOT NULL DEFAULT FALSE,
    images_only BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_job_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Indexes for common queries
CREATE INDEX idx_moderation_results_pet_id ON moderation_results(pet_id);
CREATE INDEX idx_moderation_results_status ON moderation_results(status);
CREATE INDEX idx_moderation_results_created_at ON moderation_results(created_at DESC);
CREATE INDEX idx_moderation_results_content_type ON moderation_results(content_type);

CREATE INDEX idx_flagged_content_moderation_result_id ON flagged_content(moderation_result_id);
CREATE INDEX idx_flagged_content_category ON flagged_content(category);
CREATE INDEX idx_flagged_content_severity ON flagged_content(severity);

CREATE INDEX idx_moderation_jobs_status ON moderation_jobs(status);
CREATE INDEX idx_moderation_jobs_created_at ON moderation_jobs(created_at DESC);
