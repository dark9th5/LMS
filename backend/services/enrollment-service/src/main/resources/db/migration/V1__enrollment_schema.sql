CREATE TABLE training_classes (
    id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(220) NOT NULL,
    course_id UUID NOT NULL, course_version INTEGER NOT NULL, starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ, due_at TIMESTAMPTZ, status VARCHAR(30) NOT NULL,
    created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE class_instructors (
    class_id UUID NOT NULL REFERENCES training_classes(id) ON DELETE CASCADE,
    instructor_id UUID NOT NULL,
    PRIMARY KEY(class_id, instructor_id)
);
CREATE TABLE enrollments (
    id UUID PRIMARY KEY, class_id UUID NOT NULL REFERENCES training_classes(id),
    course_id UUID NOT NULL, user_id UUID NOT NULL, due_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL, idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    enrolled_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_enrollment_class_user UNIQUE(class_id, user_id)
);
CREATE INDEX idx_enrollment_user ON enrollments(user_id);
CREATE INDEX idx_enrollment_class ON enrollments(class_id);
