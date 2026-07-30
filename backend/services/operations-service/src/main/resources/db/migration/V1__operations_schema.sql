create table if not exists operation_jobs(id uuid primary key,type varchar(30) not null,status varchar(30) not null,requested_by uuid not null,requested_at timestamptz not null,started_at timestamptz,finished_at timestamptz,parameters_json text not null,result_json text,error_message text);
create index if not exists idx_operation_job_time on operation_jobs(requested_at desc);
