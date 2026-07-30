create table if not exists grade_results (
 id uuid primary key, session_id uuid not null, exam_id uuid not null, user_id uuid not null,
 score double precision not null, max_score double precision not null, percentage double precision not null,
 passing_score double precision not null, passed boolean not null, status varchar(30) not null,
 details_json text not null, feedback text, graded_by uuid, created_at timestamptz not null,
 updated_at timestamptz not null, row_version bigint not null default 0,
 constraint uq_grade_session unique(session_id)
);
create index if not exists idx_grade_user on grade_results(user_id, created_at desc);
create index if not exists idx_grade_status on grade_results(status, created_at);
