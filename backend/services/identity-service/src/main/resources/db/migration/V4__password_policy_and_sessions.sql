ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;
UPDATE user_accounts SET must_change_password = FALSE WHERE protected_account = TRUE;

ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_reason VARCHAR(120);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMPTZ;

CREATE TABLE password_history (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_password_history_user ON password_history(user_id, created_at DESC);
