create table if not exists certificate_templates (
    id uuid primary key,
    name varchar(180) not null,
    course_id uuid,
    title varchar(240) not null,
    issuer_name varchar(240) not null,
    body_text varchar(1000) not null,
    primary_color varchar(20) not null,
    secondary_color varchar(20) not null,
    logo_url varchar(500),
    signature_name varchar(240),
    active boolean not null default true,
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0
);
create index if not exists idx_certificate_template_course on certificate_templates(course_id, active, updated_at desc);

alter table certificates add column if not exists template_id uuid;
alter table certificates add column if not exists template_snapshot_json text not null default '{}';
