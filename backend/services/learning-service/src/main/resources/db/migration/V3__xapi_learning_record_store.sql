create table if not exists xapi_statements (
    id uuid primary key,
    actor_user_id uuid not null,
    verb varchar(180) not null,
    object_id varchar(500) not null,
    object_type varchar(30) not null,
    course_id uuid,
    lesson_id uuid,
    enrollment_id uuid,
    result_score double precision,
    result_success boolean,
    result_completion boolean,
    duration_seconds bigint,
    context_json text not null default '{}',
    occurred_at timestamptz not null,
    stored_at timestamptz not null,
    source varchar(80) not null default 'WEB',
    constraint ck_xapi_score check (result_score is null or (result_score >= 0 and result_score <= 100)),
    constraint ck_xapi_duration check (duration_seconds is null or duration_seconds >= 0)
);
create index if not exists idx_xapi_actor_time on xapi_statements(actor_user_id, occurred_at desc);
create index if not exists idx_xapi_object_time on xapi_statements(object_id, occurred_at desc);
create index if not exists idx_xapi_course_time on xapi_statements(course_id, occurred_at desc);
