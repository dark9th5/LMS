CREATE TABLE learning_paths (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(220) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    owner_id UUID NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_learning_path_status ON learning_paths(status, updated_at DESC);

CREATE TABLE learning_path_items (
    id UUID PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES training_classes(id),
    course_id UUID NOT NULL,
    course_version INTEGER NOT NULL CHECK (course_version > 0),
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    required BOOLEAN NOT NULL DEFAULT TRUE,
    unlock_mode VARCHAR(30) NOT NULL CHECK (unlock_mode IN ('IMMEDIATE','AFTER_PREVIOUS')),
    due_offset_days INTEGER NOT NULL DEFAULT 0 CHECK (due_offset_days BETWEEN 0 AND 3650),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_learning_path_item_order UNIQUE(path_id, sort_order),
    CONSTRAINT uq_learning_path_item_class UNIQUE(path_id, class_id)
);
CREATE INDEX idx_learning_path_item_path ON learning_path_items(path_id, sort_order);

CREATE TABLE learning_path_assignments (
    id UUID PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    assignee_type VARCHAR(30) NOT NULL CHECK (assignee_type IN ('USER','GROUP','DEPARTMENT','BRANCH')),
    assignee_id UUID NOT NULL,
    due_at TIMESTAMPTZ,
    assigned_by UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','CANCELLED'))
);
CREATE INDEX idx_learning_path_assignment_path ON learning_path_assignments(path_id, assigned_at DESC);
CREATE INDEX idx_learning_path_assignment_target ON learning_path_assignments(assignee_type, assignee_id, status);

CREATE TABLE user_learning_paths (
    id UUID PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    source_assignment_id UUID NOT NULL REFERENCES learning_path_assignments(id),
    due_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ASSIGNED','IN_PROGRESS','COMPLETED','OVERDUE','CANCELLED')),
    assigned_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_learning_path UNIQUE(path_id, user_id)
);
CREATE INDEX idx_user_learning_path_user ON user_learning_paths(user_id, status, updated_at DESC);
