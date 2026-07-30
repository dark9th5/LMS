create table if not exists audit_entries (
 id uuid primary key, event_id uuid not null, actor_id varchar(255), actor_username varchar(255), action varchar(120) not null,
 resource_type varchar(120) not null, resource_id varchar(255), outcome varchar(40) not null,
 before_json text, after_json text, ip_address varchar(255), correlation_id varchar(255), occurred_at timestamptz not null,
 constraint uq_audit_event unique(event_id)
);
create index if not exists idx_audit_time on audit_entries(occurred_at desc);
create index if not exists idx_audit_actor on audit_entries(actor_id, occurred_at desc);
create index if not exists idx_audit_resource on audit_entries(resource_type, resource_id);
