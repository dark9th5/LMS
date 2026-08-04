create table if not exists operation_schedules (
    id uuid primary key,
    name varchar(180) not null,
    operation_type varchar(30) not null,
    frequency varchar(20) not null,
    day_of_week integer,
    hour_utc integer not null,
    parameters_json text not null default '{}',
    enabled boolean not null default true,
    next_run_at timestamptz not null,
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_operation_schedule_hour check (hour_utc between 0 and 23),
    constraint ck_operation_schedule_day check (day_of_week is null or day_of_week between 1 and 7),
    constraint ck_operation_schedule_type check (operation_type in ('BACKUP','MAINTENANCE'))
);
create index if not exists idx_operation_schedule_due on operation_schedules(enabled, next_run_at asc);
