-- Rebuild the partial index so rows left in PROCESSING after a service crash can be reclaimed.
DROP INDEX IF EXISTS idx_notifications_email_due;

CREATE INDEX idx_notifications_email_due
    ON notifications (next_attempt_at, created_at)
    WHERE channel = 'EMAIL' AND delivery_status IN ('CREATED', 'FAILED', 'PROCESSING');

-- Older workers cleared next_attempt_at when claiming. Make those rows immediately reclaimable.
UPDATE notifications
SET next_attempt_at = now(),
    last_error = COALESCE(last_error, 'Recovered after an interrupted email delivery')
WHERE channel = 'EMAIL'
  AND delivery_status = 'PROCESSING'
  AND next_attempt_at IS NULL;
