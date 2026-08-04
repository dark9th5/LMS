alter table grade_results add column if not exists enrollment_id uuid;
alter table grade_results add column if not exists lesson_id uuid;
alter table grade_results add column if not exists score_strategy varchar(20) not null default 'HIGHEST';

create index if not exists idx_grade_enrollment
    on grade_results(enrollment_id)
    where enrollment_id is not null;
create index if not exists idx_grade_lesson
    on grade_results(lesson_id)
    where lesson_id is not null;
