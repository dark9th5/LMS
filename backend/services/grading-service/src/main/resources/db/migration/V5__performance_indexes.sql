CREATE INDEX IF NOT EXISTS idx_grade_exam_status_created ON grade_results(exam_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_grade_user_exam ON grade_results(user_id, exam_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_grade_appeal_status_created ON grade_appeals(status, created_at DESC);
