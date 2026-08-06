CREATE INDEX IF NOT EXISTS idx_enrollment_user_status ON enrollments(user_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_enrollment_course_status ON enrollments(course_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assignment_course_status ON course_assignments_v2(course_id, status, assigned_at DESC);
CREATE INDEX IF NOT EXISTS idx_assignment_assignee_status ON course_assignments_v2(assignee_id, status, due_at);
CREATE INDEX IF NOT EXISTS idx_live_sessions_course_start ON live_sessions(course_id, starts_at);
