CREATE TABLE IF NOT EXISTS course_cohorts_v2 (
    id uuid PRIMARY KEY,
    course_id uuid NOT NULL,
    code varchar(80) NOT NULL,
    name varchar(240) NOT NULL,
    owner_user_id uuid NOT NULL,
    organization_unit_id uuid NULL,
    starts_at timestamptz NULL,
    ends_at timestamptz NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_course_cohort_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
    UNIQUE (course_id, code)
);

CREATE TABLE IF NOT EXISTS course_assignments_v2 (
    id uuid PRIMARY KEY,
    course_id uuid NOT NULL,
    class_id uuid NOT NULL REFERENCES training_classes(id) ON DELETE CASCADE,
    assignee_type varchar(30) NOT NULL CHECK (assignee_type IN ('USER', 'GROUP', 'DEPARTMENT', 'BRANCH')),
    assignee_id uuid NOT NULL,
    assigned_version integer NOT NULL,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    available_from timestamptz NULL,
    due_at timestamptz NULL,
    grace_period_minutes integer NOT NULL DEFAULT 0 CHECK (grace_period_minutes >= 0),
    required boolean NOT NULL DEFAULT true,
    assigned_by uuid NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_course_assignment_dates CHECK (due_at IS NULL OR available_from IS NULL OR due_at > available_from)
);
CREATE INDEX IF NOT EXISTS idx_course_assignment_course ON course_assignments_v2 (course_id);
CREATE INDEX IF NOT EXISTS idx_course_assignment_assignee ON course_assignments_v2 (assignee_type, assignee_id);
