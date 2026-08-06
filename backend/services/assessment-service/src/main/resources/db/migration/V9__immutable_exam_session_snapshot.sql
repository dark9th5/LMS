ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS questions_snapshot_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS grading_snapshot_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS passing_score_snapshot DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS auto_grade_snapshot BOOLEAN,
    ADD COLUMN IF NOT EXISTS context_type_snapshot VARCHAR(40),
    ADD COLUMN IF NOT EXISTS score_strategy_snapshot VARCHAR(20);

COMMENT ON COLUMN exam_sessions.questions_snapshot_json IS
    'Immutable candidate-facing question snapshot captured when the attempt starts.';
COMMENT ON COLUMN exam_sessions.grading_snapshot_json IS
    'Immutable correct-answer snapshot captured when the attempt starts.';
