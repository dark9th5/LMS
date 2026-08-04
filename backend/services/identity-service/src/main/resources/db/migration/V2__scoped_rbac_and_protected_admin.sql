ALTER TABLE user_accounts
    ADD COLUMN IF NOT EXISTS account_type varchar(30) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS protected_account boolean NOT NULL DEFAULT false;

-- Preserve existing deployments: choose the oldest active ADMIN as bootstrap admin.
WITH bootstrap AS (
    SELECT u.id
    FROM user_accounts u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON r.id = ur.role_id
    WHERE upper(r.code) = 'ADMIN' AND u.status = 'ACTIVE'
    ORDER BY u.created_at, u.id
    LIMIT 1
)
UPDATE user_accounts
SET account_type = 'SYSTEM_ADMIN', protected_account = true
WHERE id IN (SELECT id FROM bootstrap);

CREATE UNIQUE INDEX IF NOT EXISTS uq_single_protected_system_admin
    ON user_accounts (protected_account)
    WHERE protected_account = true;

CREATE TABLE IF NOT EXISTS authorization_grants (
    id uuid PRIMARY KEY,
    principal_type varchar(20) NOT NULL CHECK (principal_type IN ('USER', 'ROLE')),
    principal_id uuid NOT NULL,
    permission_code varchar(120) NOT NULL,
    scope_type varchar(30) NOT NULL CHECK (scope_type IN ('SYSTEM', 'BRANCH', 'DEPARTMENT', 'GROUP', 'COURSE', 'EXAM')),
    scope_id uuid NULL,
    effect varchar(10) NOT NULL CHECK (effect IN ('ALLOW', 'DENY')),
    valid_from timestamptz NULL,
    valid_until timestamptz NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_authorization_grant_scope CHECK (
        (scope_type = 'SYSTEM' AND scope_id IS NULL) OR
        (scope_type <> 'SYSTEM' AND scope_id IS NOT NULL)
    ),
    CONSTRAINT ck_authorization_grant_dates CHECK (
        valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from
    )
);

CREATE INDEX IF NOT EXISTS idx_auth_grant_principal
    ON authorization_grants (principal_type, principal_id);
CREATE INDEX IF NOT EXISTS idx_auth_grant_scope
    ON authorization_grants (scope_type, scope_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_authorization_grant_natural
    ON authorization_grants (
        principal_type, principal_id, permission_code, scope_type,
        COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid), effect,
        COALESCE(valid_from, '-infinity'::timestamptz),
        COALESCE(valid_until, 'infinity'::timestamptz)
    );

CREATE TABLE IF NOT EXISTS scoped_role_assignments (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    scope_type varchar(30) NOT NULL CHECK (scope_type IN ('SYSTEM', 'BRANCH', 'DEPARTMENT', 'GROUP', 'COURSE', 'EXAM')),
    scope_id uuid NULL,
    effect varchar(10) NOT NULL CHECK (effect IN ('ALLOW', 'DENY')),
    valid_from timestamptz NULL,
    valid_until timestamptz NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_scoped_role_scope CHECK (
        (scope_type = 'SYSTEM' AND scope_id IS NULL) OR
        (scope_type <> 'SYSTEM' AND scope_id IS NOT NULL)
    ),
    CONSTRAINT ck_scoped_role_dates CHECK (
        valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from
    )
);
CREATE INDEX IF NOT EXISTS idx_scoped_role_user ON scoped_role_assignments (user_id);
CREATE INDEX IF NOT EXISTS idx_scoped_role_scope ON scoped_role_assignments (scope_type, scope_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_scoped_role_assignment_natural
    ON scoped_role_assignments (
        user_id, role_id, scope_type,
        COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid), effect,
        COALESCE(valid_from, '-infinity'::timestamptz),
        COALESCE(valid_until, 'infinity'::timestamptz)
    );

CREATE TABLE IF NOT EXISTS bulk_operations (
    operation_id varchar(120) PRIMARY KEY,
    operation_type varchar(80) NOT NULL,
    requested_by uuid NOT NULL,
    result_json text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_bulk_operations_created_at ON bulk_operations (created_at);
