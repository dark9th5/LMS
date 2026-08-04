alter table courses add column if not exists published_version integer not null default 0;
update courses set published_version = content_version where published_at is not null and published_version = 0;

create table if not exists course_versions (
    id uuid primary key,
    course_id uuid not null references courses(id) on delete cascade,
    version_number integer not null,
    snapshot_json text not null,
    created_by uuid not null,
    created_at timestamptz not null default now(),
    constraint uq_course_version_number unique (course_id, version_number),
    constraint ck_course_version_positive check (version_number > 0)
);
create index if not exists idx_course_version_course on course_versions(course_id, version_number);
