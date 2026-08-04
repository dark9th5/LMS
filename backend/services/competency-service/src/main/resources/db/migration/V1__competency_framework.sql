CREATE TABLE competencies (
  id UUID PRIMARY KEY,
  code VARCHAR(80) NOT NULL,
  name VARCHAR(220) NOT NULL,
  description TEXT,
  category VARCHAR(120),
  max_level INTEGER NOT NULL DEFAULT 5 CHECK (max_level BETWEEN 1 AND 10),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_competency_code UNIQUE (code)
);

CREATE TABLE competency_profiles (
  id UUID PRIMARY KEY,
  code VARCHAR(80) NOT NULL,
  name VARCHAR(220) NOT NULL,
  description TEXT,
  organization_unit_id UUID,
  role_code VARCHAR(80),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_competency_profile_code UNIQUE (code)
);

CREATE TABLE competency_profile_requirements (
  id UUID PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES competency_profiles(id),
  competency_id UUID NOT NULL REFERENCES competencies(id),
  required_level INTEGER NOT NULL CHECK (required_level BETWEEN 0 AND 10),
  weight DOUBLE PRECISION NOT NULL DEFAULT 1 CHECK (weight >= 0),
  CONSTRAINT uq_profile_competency UNIQUE (profile_id, competency_id)
);

CREATE TABLE user_competency_profiles (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  profile_id UUID NOT NULL REFERENCES competency_profiles(id),
  assigned_by UUID NOT NULL,
  assigned_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_user_competency_profile UNIQUE (user_id, profile_id)
);

CREATE TABLE user_competency_assessments (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  competency_id UUID NOT NULL REFERENCES competencies(id),
  level INTEGER NOT NULL CHECK (level BETWEEN 0 AND 10),
  source VARCHAR(20) NOT NULL,
  assessed_by UUID NOT NULL,
  evidence_json TEXT NOT NULL DEFAULT '{}',
  assessed_at TIMESTAMPTZ NOT NULL,
  valid_until TIMESTAMPTZ
);
CREATE INDEX idx_user_competency_assessment ON user_competency_assessments(user_id, competency_id, assessed_at DESC);

CREATE TABLE course_competency_maps (
  id UUID PRIMARY KEY,
  course_id UUID NOT NULL,
  competency_id UUID NOT NULL REFERENCES competencies(id),
  target_level INTEGER NOT NULL CHECK (target_level BETWEEN 1 AND 10),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_course_competency UNIQUE (course_id, competency_id)
);
