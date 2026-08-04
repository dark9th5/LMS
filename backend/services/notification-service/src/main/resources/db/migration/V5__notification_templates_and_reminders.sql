create table if not exists notification_templates (
    id uuid primary key,
    code varchar(80) not null,
    name varchar(180) not null,
    event_type varchar(120),
    title_template varchar(240) not null,
    body_template text not null,
    in_app_enabled boolean not null default true,
    email_enabled boolean not null default false,
    active boolean not null default true,
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint uq_notification_template_code unique(code)
);
create index if not exists idx_notification_template_event on notification_templates(event_type, active);

create table if not exists notification_reminder_rules (
    id uuid primary key,
    name varchar(180) not null,
    rule_type varchar(30) not null,
    template_id uuid not null references notification_templates(id),
    relative_days integer not null,
    hour_utc integer not null,
    enabled boolean not null default true,
    next_run_at timestamptz not null,
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_notification_reminder_relative_days check (relative_days between -365 and 365),
    constraint ck_notification_reminder_hour check (hour_utc between 0 and 23)
);
create index if not exists idx_notification_reminder_due on notification_reminder_rules(enabled, next_run_at asc);

create table if not exists notification_reminder_dispatches (
    id uuid primary key,
    rule_id uuid not null references notification_reminder_rules(id) on delete cascade,
    enrollment_id uuid not null,
    user_id uuid not null,
    due_at timestamptz not null,
    source_event_id uuid not null,
    dispatched_at timestamptz not null,
    constraint uq_notification_reminder_dispatch unique(rule_id, enrollment_id, due_at)
);
create index if not exists idx_notification_reminder_dispatch_user on notification_reminder_dispatches(user_id, dispatched_at desc);

-- Safe starter assets: the rule is disabled until an administrator reviews the
-- wording, SMTP configuration and desired delivery hour.
insert into notification_templates (
    id, code, name, event_type, title_template, body_template,
    in_app_enabled, email_enabled, active, created_by, created_at, updated_at, version
) values (
    '741050d8-9f7c-4eca-9634-cf1b8b935a01',
    'COURSE_DUE_REMINDER',
    'Nhắc hạn hoàn thành khóa học',
    null,
    'Khóa học sắp đến hạn',
    'Hạn hoàn thành: {{dueDate}}. Tiến độ hiện tại: {{progressPercent}}%. Hãy tiếp tục hành trình học tập của bạn.',
    true, false, true,
    '00000000-0000-0000-0000-000000000000', now(), now(), 0
) on conflict (code) do nothing;

insert into notification_reminder_rules (
    id, name, rule_type, template_id, relative_days, hour_utc, enabled,
    next_run_at, created_by, created_at, updated_at, version
) values (
    '741050d8-9f7c-4eca-9634-cf1b8b935a02',
    'Nhắc trước hạn 7 ngày',
    'COURSE_DUE',
    '741050d8-9f7c-4eca-9634-cf1b8b935a01',
    7, 0, false,
    date_trunc('day', now()) + interval '1 day',
    '00000000-0000-0000-0000-000000000000', now(), now(), 0
) on conflict (id) do nothing;
