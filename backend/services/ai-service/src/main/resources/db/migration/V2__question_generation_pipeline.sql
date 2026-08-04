CREATE TABLE IF NOT EXISTS ai_provider_configs (
    id uuid PRIMARY KEY,
    code varchar(80) NOT NULL UNIQUE,
    provider_type varchar(40) NOT NULL CHECK (provider_type IN ('LOCAL_OPENAI_COMPATIBLE', 'REMOTE_OPENAI_COMPATIBLE', 'CUSTOM_ADAPTER')),
    base_url varchar(1000) NOT NULL,
    model varchar(240) NOT NULL,
    enabled boolean NOT NULL DEFAULT false,
    encrypted_api_key bytea NULL,
    secret_key_version integer NULL,
    request_timeout_seconds integer NOT NULL DEFAULT 120 CHECK (request_timeout_seconds BETWEEN 5 AND 3600),
    max_output_tokens integer NULL CHECK (max_output_tokens IS NULL OR max_output_tokens > 0),
    config_json text NOT NULL DEFAULT '{}',
    updated_by uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS question_generation_jobs (
    id uuid PRIMARY KEY,
    course_id uuid NOT NULL,
    requested_by uuid NOT NULL,
    provider_config_id uuid NOT NULL REFERENCES ai_provider_configs(id),
    document_version_ids_json text NOT NULL,
    generation_options_json text NOT NULL DEFAULT '{}',
    status varchar(30) NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'EXTRACTING', 'GENERATING', 'VALIDATING', 'REVIEW_REQUIRED', 'APPROVED', 'IMPORTED', 'FAILED')),
    question_set_json text NULL,
    validation_errors_json text NULL,
    error_message varchar(2000) NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz NULL
);
CREATE INDEX IF NOT EXISTS idx_question_generation_status ON question_generation_jobs (status, created_at);

CREATE TABLE IF NOT EXISTS question_generation_reviews (
    id uuid PRIMARY KEY,
    job_id uuid NOT NULL REFERENCES question_generation_jobs(id) ON DELETE CASCADE,
    reviewer_id uuid NOT NULL,
    decision varchar(20) NOT NULL CHECK (decision IN ('APPROVE', 'REJECT', 'REQUEST_CHANGES')),
    comments text NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
