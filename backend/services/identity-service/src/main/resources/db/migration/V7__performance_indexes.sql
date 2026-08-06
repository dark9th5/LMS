CREATE INDEX IF NOT EXISTS idx_user_accounts_status_name ON user_accounts(status, full_name);
CREATE INDEX IF NOT EXISTS idx_user_accounts_org_status ON user_accounts(organization_unit_id, status);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active_user ON refresh_tokens(user_id, revoked_at, expires_at);
