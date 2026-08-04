ALTER TABLE operation_jobs
    ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(160),
    ADD COLUMN IF NOT EXISTS claim_token VARCHAR(120),
    ADD COLUMN IF NOT EXISTS heartbeat_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_operation_jobs_claimable
    ON operation_jobs (status, requested_at, lease_until);
