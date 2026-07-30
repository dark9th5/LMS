create table if not exists certificates (
 id uuid primary key, enrollment_id uuid not null, course_id uuid not null, user_id uuid not null,
 verification_code varchar(48) not null unique, generation integer not null, status varchar(20) not null,
 issued_at timestamptz not null, revoked_at timestamptz, revoke_reason text, replaces_certificate_id uuid,
 constraint uq_certificate_enrollment_active unique(enrollment_id, generation)
);
create index if not exists idx_certificate_user on certificates(user_id, issued_at desc);
