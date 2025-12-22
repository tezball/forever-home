-- Add job_id column to link moderation results to their batch job
ALTER TABLE moderation_results ADD COLUMN job_id UUID REFERENCES moderation_jobs(id);

-- Index for efficient lookup of results by job
CREATE INDEX idx_moderation_results_job_id ON moderation_results(job_id);
