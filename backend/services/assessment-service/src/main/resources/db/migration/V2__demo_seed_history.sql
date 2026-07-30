CREATE TABLE IF NOT EXISTS demo_seed_history (
    seed_key VARCHAR(120) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL
);
