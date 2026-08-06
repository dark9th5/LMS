CREATE INDEX IF NOT EXISTS idx_org_path_sort ON organization_units(materialized_path, sort_order);
CREATE INDEX IF NOT EXISTS idx_org_parent_sort ON organization_units(parent_id, sort_order, name);
CREATE INDEX IF NOT EXISTS idx_org_membership_unit_dates ON organization_memberships_v2(unit_id, valid_until, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_org_membership_user_dates ON organization_memberships_v2(user_id, valid_until, created_at DESC);
