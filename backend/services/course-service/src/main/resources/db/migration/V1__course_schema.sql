CREATE TABLE course_categories (
    id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(180) NOT NULL,
    parent_id UUID REFERENCES course_categories(id), status VARCHAR(20) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE courses (
    id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(240) NOT NULL,
    description TEXT, objectives TEXT, target_audience VARCHAR(500), duration_minutes INTEGER,
    passing_score DOUBLE PRECISION NOT NULL, completion_policy_json TEXT NOT NULL,
    category_id UUID REFERENCES course_categories(id), status VARCHAR(30) NOT NULL,
    content_version INTEGER NOT NULL, published_at TIMESTAMPTZ, published_by UUID,
    owner_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE lessons (
    id UUID PRIMARY KEY, course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(220) NOT NULL, type VARCHAR(30) NOT NULL, text_content TEXT, file_id UUID,
    required BOOLEAN NOT NULL DEFAULT TRUE, sort_order INTEGER NOT NULL,
    estimated_minutes INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lesson_course_order UNIQUE(course_id, sort_order)
);
CREATE INDEX idx_course_status ON courses(status);
CREATE INDEX idx_course_owner ON courses(owner_id);
CREATE INDEX idx_lesson_course ON lessons(course_id);
