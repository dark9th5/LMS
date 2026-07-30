CREATE TABLE questions (
    id UUID PRIMARY KEY, owner_id UUID NOT NULL, type VARCHAR(30) NOT NULL, prompt TEXT NOT NULL,
    options_json TEXT NOT NULL, correct_answers_json TEXT NOT NULL, explanation TEXT,
    difficulty INTEGER NOT NULL, tags_csv VARCHAR(500) NOT NULL, default_points DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL, question_version INTEGER NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, row_version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE exams (
    id UUID PRIMARY KEY, title VARCHAR(220) NOT NULL, course_id UUID, lesson_id UUID,
    duration_minutes INTEGER NOT NULL, opens_at TIMESTAMPTZ, closes_at TIMESTAMPTZ,
    max_attempts INTEGER NOT NULL, wait_minutes_between_attempts INTEGER NOT NULL,
    passing_score DOUBLE PRECISION NOT NULL, shuffle_questions BOOLEAN NOT NULL,
    shuffle_answers BOOLEAN NOT NULL, score_strategy VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,
    exam_version INTEGER NOT NULL, owner_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, row_version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE exam_questions (
    id UUID PRIMARY KEY, exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id UUID NOT NULL, question_version INTEGER NOT NULL, type VARCHAR(30) NOT NULL,
    prompt_snapshot TEXT NOT NULL, options_snapshot_json TEXT NOT NULL,
    correct_answers_snapshot_json TEXT NOT NULL, points DOUBLE PRECISION NOT NULL,
    sort_order INTEGER NOT NULL, CONSTRAINT uq_exam_question UNIQUE(exam_id, question_id)
);
CREATE TABLE exam_sessions (
    id UUID PRIMARY KEY, exam_id UUID NOT NULL REFERENCES exams(id), exam_version INTEGER NOT NULL,
    user_id UUID NOT NULL, attempt_no INTEGER NOT NULL, status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL, submitted_at TIMESTAMPTZ,
    answers_json TEXT NOT NULL, submit_idempotency_key VARCHAR(160) UNIQUE,
    updated_at TIMESTAMPTZ NOT NULL, row_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_exam_session_user ON exam_sessions(user_id, exam_id);
