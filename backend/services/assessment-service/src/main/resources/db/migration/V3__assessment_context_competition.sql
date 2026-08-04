CREATE TABLE IF NOT EXISTS assessment_contexts (
    assessment_id uuid PRIMARY KEY,
    context_type varchar(30) NOT NULL CHECK (context_type IN ('COURSE_QUIZ', 'COURSE_ASSIGNMENT', 'STANDALONE_EXAM', 'COMPETITION')),
    course_id uuid NULL,
    cohort_id uuid NULL,
    opens_at timestamptz NULL,
    closes_at timestamptz NULL,
    max_attempts integer NOT NULL DEFAULT 1 CHECK (max_attempts > 0),
    auto_grade boolean NOT NULL DEFAULT true,
    CONSTRAINT ck_assessment_context_course CHECK (
        (context_type IN ('COURSE_QUIZ', 'COURSE_ASSIGNMENT') AND course_id IS NOT NULL) OR
        (context_type IN ('STANDALONE_EXAM', 'COMPETITION') AND course_id IS NULL)
    ),
    CONSTRAINT ck_assessment_window CHECK (closes_at IS NULL OR opens_at IS NULL OR closes_at > opens_at)
);

CREATE TABLE IF NOT EXISTS competitions (
    assessment_id uuid PRIMARY KEY REFERENCES assessment_contexts(assessment_id) ON DELETE CASCADE,
    registration_opens_at timestamptz NULL,
    registration_closes_at timestamptz NULL,
    leaderboard_visibility varchar(30) NOT NULL DEFAULT 'AFTER_CLOSE',
    tie_break_rule varchar(80) NOT NULL DEFAULT 'SCORE_DURATION_SUBMITTED_AT',
    result_status varchar(20) NOT NULL DEFAULT 'PROVISIONAL',
    published_at timestamptz NULL,
    published_by uuid NULL
);

CREATE TABLE IF NOT EXISTS competition_leaderboard (
    competition_id uuid NOT NULL REFERENCES competitions(assessment_id) ON DELETE CASCADE,
    user_id uuid NOT NULL,
    attempt_id uuid NOT NULL,
    score numeric(10,4) NOT NULL,
    duration_ms bigint NOT NULL CHECK (duration_ms >= 0),
    submitted_at timestamptz NOT NULL,
    rank integer NULL CHECK (rank IS NULL OR rank > 0),
    calculated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (competition_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_competition_ranking
    ON competition_leaderboard (competition_id, score DESC, duration_ms ASC, submitted_at ASC, user_id ASC);

CREATE TABLE IF NOT EXISTS competition_rewards (
    id uuid PRIMARY KEY,
    competition_id uuid NOT NULL REFERENCES competitions(assessment_id),
    rank_from integer NOT NULL CHECK (rank_from > 0),
    rank_to integer NOT NULL CHECK (rank_to >= rank_from),
    reward_type varchar(40) NOT NULL,
    reward_payload_json text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reward_ledger (
    id uuid PRIMARY KEY,
    competition_id uuid NOT NULL,
    user_id uuid NOT NULL,
    reward_id uuid NOT NULL REFERENCES competition_rewards(id),
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    issued_at timestamptz NULL,
    external_reference varchar(160) NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (competition_id, user_id, reward_id)
);
