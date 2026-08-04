CREATE TABLE IF NOT EXISTS question_provenance (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    course_id UUID NOT NULL,
    external_id VARCHAR(240) NOT NULL,
    citations_json TEXT NOT NULL DEFAULT '[]',
    source_document_versions_json TEXT NOT NULL DEFAULT '[]',
    generator_metadata_json TEXT NOT NULL DEFAULT '{}',
    imported_by UUID NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_question_provenance_question UNIQUE (question_id)
);
CREATE INDEX IF NOT EXISTS idx_question_provenance_course ON question_provenance(course_id, imported_at DESC);
