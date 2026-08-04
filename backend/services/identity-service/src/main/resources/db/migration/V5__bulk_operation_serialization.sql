insert into identity_system_locks(lock_name)
values ('IDENTITY_BULK_OPERATION_SERIALIZATION')
on conflict (lock_name) do nothing;
