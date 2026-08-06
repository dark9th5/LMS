CREATE INDEX IF NOT EXISTS idx_questions_owner_status_updated ON questions(owner_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_exams_owner_status_updated ON exams(owner_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_exams_course_status ON exams(course_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_exams_window ON exams(status, opens_at, closes_at);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_exam_status ON exam_sessions(exam_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_user_status ON exam_sessions(user_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assessment_assignment_user_status ON assessment_assignments(assignee_id, status, due_at);
