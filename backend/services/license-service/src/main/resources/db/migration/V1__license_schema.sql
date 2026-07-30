create table if not exists licenses (
 id uuid primary key, license_id varchar(100) not null unique, organization varchar(220) not null, edition varchar(80) not null,
 max_users integer not null, features_json text not null, issued_at timestamptz not null, expires_at timestamptz,
 status varchar(30) not null, source_payload text not null, activated_at timestamptz not null
);
