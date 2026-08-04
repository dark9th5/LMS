create table if not exists grade_revisions (
    id uuid primary key,
    grade_id uuid not null,
    previous_score double precision not null,
    new_score double precision not null,
    previous_percentage double precision not null,
    new_percentage double precision not null,
    type varchar(40) not null,
    reason text not null,
    changed_by uuid not null,
    created_at timestamptz not null,
    constraint ck_grade_revision_scores check (previous_score >= 0 and new_score >= 0)
);
create index if not exists idx_grade_revision_grade on grade_revisions(grade_id, created_at desc);

create table if not exists grade_appeals (
    id uuid primary key,
    grade_id uuid not null,
    user_id uuid not null,
    reason text not null,
    status varchar(30) not null,
    active_key varchar(20) not null,
    resolution text,
    resolved_by uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    resolved_at timestamptz,
    row_version bigint not null default 0,
    constraint uq_open_grade_appeal unique(grade_id, user_id, active_key)
);
create index if not exists idx_grade_appeal_status on grade_appeals(status, created_at asc);
create index if not exists idx_grade_appeal_user on grade_appeals(user_id, created_at desc);
