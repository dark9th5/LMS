CREATE INDEX IF NOT EXISTS idx_report_course_updated ON learner_course_read_model(course_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_report_user_activity ON learner_course_read_model(user_id, last_activity_at DESC);
CREATE INDEX IF NOT EXISTS idx_report_completion ON learner_course_read_model(completed, updated_at DESC);
