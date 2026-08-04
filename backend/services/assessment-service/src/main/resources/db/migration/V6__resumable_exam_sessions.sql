alter table exam_sessions add column if not exists grace_until timestamptz;
alter table exam_sessions add column if not exists last_heartbeat_at timestamptz;
alter table exam_sessions add column if not exists suspicious_event_count integer not null default 0;
update exam_sessions set grace_until = expires_at + interval '2 minutes' where grace_until is null;
update exam_sessions set last_heartbeat_at = updated_at where last_heartbeat_at is null;
alter table exam_sessions alter column grace_until set not null;
alter table exam_sessions alter column last_heartbeat_at set not null;

create table if not exists exam_session_events (
    id uuid primary key,
    session_id uuid not null references exam_sessions(id) on delete cascade,
    user_id uuid not null,
    type varchar(40) not null,
    details text,
    occurred_at timestamptz not null,
    stored_at timestamptz not null
);
create index if not exists idx_exam_session_event on exam_session_events(session_id, occurred_at asc);
