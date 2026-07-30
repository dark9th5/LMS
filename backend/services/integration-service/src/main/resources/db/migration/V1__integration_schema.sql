create table if not exists integration_adapters (
 id uuid primary key, code varchar(100) not null unique, name varchar(180) not null, type varchar(40) not null,
 endpoint varchar(1000) not null, mapping_json text not null, secret_reference text not null, status varchar(30) not null,
 last_tested_at timestamptz, last_test_result text, updated_at timestamptz not null, row_version bigint not null default 0
);
