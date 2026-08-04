CREATE TABLE IF NOT EXISTS live_sessions (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES training_classes(id) ON DELETE CASCADE,
    course_id UUID NOT NULL,
    title VARCHAR(220) NOT NULL,
    provider VARCHAR(120) NOT NULL DEFAULT 'EXTERNAL',
    join_url VARCHAR(2000) NOT NULL,
    host_url VARCHAR(2000),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_live_session_window CHECK (ends_at > starts_at)
);
CREATE INDEX IF NOT EXISTS idx_live_session_class_time ON live_sessions(class_id, starts_at);
