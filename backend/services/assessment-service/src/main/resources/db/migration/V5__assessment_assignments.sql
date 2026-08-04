CREATE TABLE IF NOT EXISTS assessment_assignments (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessment_contexts(assessment_id) ON DELETE CASCADE,
    assignee_type VARCHAR(30) NOT NULL CHECK (assignee_type IN ('USER', 'GROUP', 'DEPARTMENT', 'BRANCH')),
    assignee_id UUID NOT NULL,
    available_from TIMESTAMPTZ NULL,
    due_at TIMESTAMPTZ NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED')),
    assigned_by UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_assessment_assignment_target UNIQUE (assessment_id, assignee_type, assignee_id),
    CONSTRAINT ck_assessment_assignment_window CHECK (due_at IS NULL OR available_from IS NULL OR due_at > available_from)
);

CREATE INDEX IF NOT EXISTS idx_assessment_assignment_assessment
    ON assessment_assignments(assessment_id, status, assigned_at DESC);
CREATE INDEX IF NOT EXISTS idx_assessment_assignment_target
    ON assessment_assignments(assignee_type, assignee_id, status, available_from, due_at);
