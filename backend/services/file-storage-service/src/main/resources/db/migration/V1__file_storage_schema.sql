create table if not exists stored_files (
 id uuid primary key, owner_id uuid not null, original_name varchar(260) not null, storage_key varchar(260) not null unique,
 content_type varchar(160) not null, size_bytes bigint not null, sha256 varchar(64) not null, purpose varchar(80) not null,
 status varchar(30) not null, created_at timestamptz not null, deleted_at timestamptz
);
create index if not exists idx_stored_file_owner on stored_files(owner_id, created_at desc);
create index if not exists idx_stored_file_sha on stored_files(sha256);
