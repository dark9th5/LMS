create table if not exists identity_system_locks (
    lock_name varchar(120) primary key,
    created_at timestamptz not null default now()
);

insert into identity_system_locks(lock_name)
values ('ACTIVE_USER_LICENSE_CAPACITY'),
       ('USER_FILE_IMPORT_SERIALIZATION')
on conflict (lock_name) do nothing;
