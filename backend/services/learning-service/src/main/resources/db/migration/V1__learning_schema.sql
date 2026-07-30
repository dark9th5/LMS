CREATE TABLE course_progress (
    id UUID PRIMARY KEY, enrollment_id UUID NOT NULL UNIQUE, course_id UUID NOT NULL,
    user_id UUID NOT NULL, progress_percent INTEGER NOT NULL, status VARCHAR(30) NOT NULL,
    last_lesson_id UUID, last_position VARCHAR(500), total_learning_seconds BIGINT NOT NULL,
    started_at TIMESTAMPTZ, last_accessed_at TIMESTAMPTZ, completed_at TIMESTAMPTZ,
    completion_event_published BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY, enrollment_id UUID NOT NULL, course_id UUID NOT NULL,
    lesson_id UUID NOT NULL, user_id UUID NOT NULL, completed BOOLEAN NOT NULL,
    learning_seconds BIGINT NOT NULL, position VARCHAR(500), opened_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lesson_progress UNIQUE(enrollment_id, lesson_id)
);
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(160) PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_course_progress_user ON course_progress(user_id);
CREATE INDEX idx_lesson_progress_enrollment ON lesson_progress(enrollment_id);
