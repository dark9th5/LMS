ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS recipient_email VARCHAR(320),
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ;

UPDATE notifications
SET next_attempt_at = created_at
WHERE channel = 'EMAIL'
  AND delivery_status IN ('CREATED', 'FAILED')
  AND next_attempt_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_email_due
    ON notifications (next_attempt_at, created_at)
    WHERE channel = 'EMAIL' AND delivery_status IN ('CREATED', 'FAILED', 'PROCESSING');
