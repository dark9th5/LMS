create table if not exists file_access_grants (
    id uuid primary key,
    file_id uuid not null references stored_files(id) on delete cascade,
    user_id uuid not null,
    source varchar(80) not null,
    expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_file_access_grant unique(file_id, user_id)
);

create index if not exists idx_file_access_grant_user
    on file_access_grants(user_id, expires_at);
