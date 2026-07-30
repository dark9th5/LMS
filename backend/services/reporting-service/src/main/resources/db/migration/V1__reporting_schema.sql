create table if not exists report_events (
 id uuid primary key, event_id uuid not null, event_type varchar(120) not null, aggregate_id varchar(120) not null,
 occurred_at timestamptz not null, payload_json text not null, constraint uq_report_event unique(event_id)
);
create table if not exists learner_course_read_model (
 id uuid primary key, enrollment_id uuid not null, class_id uuid not null, course_id uuid not null, user_id uuid not null,
 due_at timestamptz, progress_percent integer not null default 0, completed boolean not null default false,
 completed_at timestamptz, last_activity_at timestamptz, last_score double precision, passed boolean,
 updated_at timestamptz not null, constraint uq_report_enrollment unique(enrollment_id)
);
create index if not exists idx_report_user on learner_course_read_model(user_id);
create index if not exists idx_report_course on learner_course_read_model(course_id);
