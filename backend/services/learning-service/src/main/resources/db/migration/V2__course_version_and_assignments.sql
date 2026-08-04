alter table course_progress add column if not exists course_version integer not null default 1;

create table if not exists assignment_submissions (
    id uuid primary key,
    enrollment_id uuid not null,
    class_id uuid not null,
    course_id uuid not null,
    course_version integer not null,
    lesson_id uuid not null,
    user_id uuid not null,
    attempt_number integer not null,
    file_id uuid not null,
    comment text,
    submitted_at timestamptz not null,
    late boolean not null default false,
    status varchar(30) not null,
    score double precision,
    max_score double precision,
    feedback text,
    graded_by uuid,
    graded_at timestamptz,
    idempotency_key varchar(160) not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint uq_assignment_submission_attempt unique(enrollment_id, lesson_id, attempt_number),
    constraint uq_assignment_submission_idempotency unique(idempotency_key),
    constraint ck_assignment_attempt_positive check (attempt_number > 0),
    constraint ck_assignment_score check (score is null or (score >= 0 and max_score is not null and max_score > 0 and score <= max_score))
);
create index if not exists idx_assignment_submission_user on assignment_submissions(user_id, submitted_at desc);
create index if not exists idx_assignment_submission_class on assignment_submissions(class_id, lesson_id, submitted_at desc);
