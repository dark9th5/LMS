alter table grade_results add column if not exists course_id uuid;
create index if not exists idx_grade_course_user on grade_results(course_id, user_id);
