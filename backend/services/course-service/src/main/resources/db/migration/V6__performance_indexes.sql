CREATE INDEX IF NOT EXISTS idx_courses_status_updated ON courses(status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_courses_owner_status ON courses(owner_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_courses_category_status ON courses(category_id, status);
CREATE INDEX IF NOT EXISTS idx_lessons_course_sort ON lessons(course_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_course_documents_course ON course_document_links(course_id, created_at DESC);
