CREATE TABLE organization_units (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    type VARCHAR(40) NOT NULL,
    parent_id UUID REFERENCES organization_units(id),
    status VARCHAR(30) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    materialized_path VARCHAR(1200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_org_parent ON organization_units(parent_id);
CREATE INDEX idx_org_status ON organization_units(status);
