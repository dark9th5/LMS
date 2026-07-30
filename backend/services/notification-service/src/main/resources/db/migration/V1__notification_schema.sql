create table if not exists notifications (
 id uuid primary key, source_event_id uuid not null, user_id uuid not null, type varchar(120) not null,
 title varchar(240) not null, body text not null, channel varchar(20) not null, delivery_status varchar(20) not null,
 read boolean not null default false, read_at timestamptz, created_at timestamptz not null, delivered_at timestamptz, last_error varchar(255),
 constraint uq_notification_event_user unique(source_event_id, user_id, channel)
);
create index if not exists idx_notification_user on notifications(user_id, channel, created_at desc);
