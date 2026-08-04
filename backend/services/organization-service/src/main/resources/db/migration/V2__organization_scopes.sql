CREATE TABLE IF NOT EXISTS organization_memberships_v2 (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    membership_type varchar(30) NOT NULL DEFAULT 'MEMBER',
    primary_membership boolean NOT NULL DEFAULT false,
    valid_from timestamptz NULL,
    valid_until timestamptz NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_org_membership_dates CHECK (
        valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_org_membership_active
    ON organization_memberships_v2 (user_id, unit_id, membership_type, COALESCE(valid_until, 'infinity'::timestamptz));
CREATE INDEX IF NOT EXISTS idx_org_membership_user ON organization_memberships_v2 (user_id);
CREATE INDEX IF NOT EXISTS idx_org_membership_unit ON organization_memberships_v2 (unit_id);

CREATE TABLE IF NOT EXISTS organization_scope_projection (
    ancestor_unit_id uuid NOT NULL,
    descendant_unit_id uuid NOT NULL,
    depth integer NOT NULL CHECK (depth >= 0),
    PRIMARY KEY (ancestor_unit_id, descendant_unit_id)
);
