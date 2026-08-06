CREATE INDEX IF NOT EXISTS idx_course_progress_user_status ON course_progress(user_id, status, last_accessed_at DESC);
CREATE INDEX IF NOT EXISTS idx_course_progress_course_status ON course_progress(course_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_user_course ON lesson_progress(user_id, course_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assignment_submission_course_status ON assignment_submissions(course_id, status, updated_at DESC);
