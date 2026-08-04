create table if not exists report_schedules (
    id uuid primary key,
    owner_id uuid not null,
    name varchar(180) not null,
    scope varchar(20) not null,
    frequency varchar(20) not null,
    day_of_week integer,
    hour_utc integer not null,
    enabled boolean not null default true,
    next_run_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_report_schedule_hour check (hour_utc between 0 and 23),
    constraint ck_report_schedule_day check (day_of_week is null or day_of_week between 1 and 7)
);
create index if not exists idx_report_schedule_due on report_schedules(enabled, next_run_at asc);

create table if not exists report_export_jobs (
    id uuid primary key,
    owner_id uuid not null,
    scope varchar(20) not null,
    status varchar(20) not null,
    schedule_id uuid references report_schedules(id) on delete set null,
    period_key varchar(80),
    output_csv text,
    row_count integer,
    error_message varchar(1000),
    created_at timestamptz not null,
    started_at timestamptz,
    completed_at timestamptz,
    expires_at timestamptz not null,
    version bigint not null default 0,
    constraint uq_report_schedule_period unique(schedule_id, period_key)
);
create index if not exists idx_report_export_owner on report_export_jobs(owner_id, created_at desc);
create index if not exists idx_report_export_status on report_export_jobs(status, created_at asc);
