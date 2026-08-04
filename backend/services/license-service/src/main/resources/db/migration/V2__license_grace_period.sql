alter table licenses add column if not exists grace_period_days integer not null default 0;
alter table licenses drop constraint if exists ck_license_grace_period_days;
alter table licenses add constraint ck_license_grace_period_days check (grace_period_days between 0 and 3650);
alter table licenses drop constraint if exists ck_license_max_users;
alter table licenses add constraint ck_license_max_users check (max_users > 0);
