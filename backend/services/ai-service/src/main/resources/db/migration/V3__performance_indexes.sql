CREATE INDEX IF NOT EXISTS idx_ai_provider_enabled ON ai_provider_configs(enabled, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_requester_created ON question_generation_jobs(requested_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_status_updated ON question_generation_jobs(status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_reviews_job_created ON question_generation_reviews(job_id, created_at DESC);
