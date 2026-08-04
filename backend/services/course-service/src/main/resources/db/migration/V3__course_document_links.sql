CREATE TABLE IF NOT EXISTS course_document_links (
    id uuid PRIMARY KEY,
    course_id uuid NOT NULL,
    lesson_id uuid NULL,
    file_id uuid NOT NULL,
    file_version_id uuid NOT NULL,
    purpose varchar(30) NOT NULL DEFAULT 'LEARNING_MATERIAL',
    editable boolean NOT NULL DEFAULT false,
    required boolean NOT NULL DEFAULT false,
    sort_order integer NOT NULL DEFAULT 0,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (course_id, file_version_id)
);
CREATE INDEX IF NOT EXISTS idx_course_document_course ON course_document_links (course_id, sort_order);
