CREATE TABLE IF NOT EXISTS file_versions_v2 (
    id uuid PRIMARY KEY,
    file_id uuid NOT NULL,
    version_number integer NOT NULL CHECK (version_number > 0),
    storage_key varchar(1000) NOT NULL,
    media_type varchar(255) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes >= 0),
    sha256 varchar(64) NOT NULL,
    source_type varchar(30) NOT NULL DEFAULT 'UPLOAD' CHECK (source_type IN ('UPLOAD', 'DOCX_EDIT', 'PDF_ANNOTATION', 'CONVERSION')),
    parent_version_id uuid NULL,
    change_summary varchar(1000) NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (file_id, version_number)
);
CREATE INDEX IF NOT EXISTS idx_file_versions_file ON file_versions_v2 (file_id, version_number DESC);

CREATE TABLE IF NOT EXISTS file_edit_sessions (
    id uuid PRIMARY KEY,
    file_id uuid NOT NULL,
    base_version_id uuid NOT NULL REFERENCES file_versions_v2(id),
    editor_type varchar(30) NOT NULL CHECK (editor_type IN ('ONLYOFFICE', 'COLLABORA', 'PDF_ANNOTATOR')),
    user_id uuid NOT NULL,
    lock_token_hash varchar(64) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'SAVED', 'CANCELLED', 'EXPIRED')),
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    closed_at timestamptz NULL
);
CREATE INDEX IF NOT EXISTS idx_file_edit_session_open ON file_edit_sessions (file_id, status, expires_at);
