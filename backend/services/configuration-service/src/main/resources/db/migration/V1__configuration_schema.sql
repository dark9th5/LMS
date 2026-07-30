create table if not exists product_configuration (
 id uuid primary key, product_name varchar(160) not null, logo_url varchar(500) not null, primary_color varchar(20) not null,
 accent_color varchar(20) not null, default_locale varchar(20) not null, feature_flags_json text not null,
 terminology_json text not null, updated_at timestamptz not null, updated_by uuid, row_version bigint not null default 0
);
insert into product_configuration(id, product_name, logo_url, primary_color, accent_color, default_locale, feature_flags_json, terminology_json, updated_at, row_version)
values ('00000000-0000-0000-0000-000000000001','LMSPilot','','#1457D9','#15A37B','vi','{"AI":false,"LDAP":false}','{}',now(),0)
on conflict (id) do nothing;
