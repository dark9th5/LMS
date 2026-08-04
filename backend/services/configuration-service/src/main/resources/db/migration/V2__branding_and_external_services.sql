CREATE TABLE IF NOT EXISTS branding_profiles (
    id uuid PRIMARY KEY,
    profile_key varchar(80) NOT NULL UNIQUE DEFAULT 'default',
    system_name varchar(240) NOT NULL,
    introduction text NULL,
    logo_file_id uuid NULL,
    favicon_file_id uuid NULL,
    background_file_id uuid NULL,
    primary_color varchar(9) NOT NULL DEFAULT '#2563EB',
    secondary_color varchar(9) NOT NULL DEFAULT '#1E40AF',
    background_color varchar(9) NOT NULL DEFAULT '#FFFFFF',
    text_color varchar(9) NOT NULL DEFAULT '#111827',
    custom_domain varchar(253) NULL,
    updated_by uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_branding_primary_color CHECK (primary_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$'),
    CONSTRAINT ck_branding_secondary_color CHECK (secondary_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$'),
    CONSTRAINT ck_branding_background_color CHECK (background_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$'),
    CONSTRAINT ck_branding_text_color CHECK (text_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$')
);

CREATE TABLE IF NOT EXISTS external_service_configs (
    id uuid PRIMARY KEY,
    service_type varchar(40) NOT NULL CHECK (service_type IN ('REDIS', 'SMTP', 'VIDEO_CONFERENCE', 'AI_PROVIDER', 'OBJECT_STORAGE', 'DOCUMENT_EDITOR')),
    config_key varchar(80) NOT NULL DEFAULT 'default',
    enabled boolean NOT NULL DEFAULT false,
    config_json text NOT NULL DEFAULT '{}',
    encrypted_secret bytea NULL,
    secret_key_version integer NULL,
    health_status varchar(20) NOT NULL DEFAULT 'UNKNOWN',
    last_checked_at timestamptz NULL,
    last_error varchar(1000) NULL,
    updated_by uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (service_type, config_key)
);
