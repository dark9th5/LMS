alter table exam_sessions add column if not exists enrollment_id uuid;
alter table exam_sessions add column if not exists course_id uuid;
alter table exam_sessions add column if not exists lesson_id uuid;

update exam_sessions session
set course_id = exam.course_id,
    lesson_id = exam.lesson_id
from exams exam
where session.exam_id = exam.id
  and (session.course_id is null or session.lesson_id is null);

create unique index if not exists uq_exam_enrollment_attempt
    on exam_sessions(exam_id, enrollment_id, attempt_no)
    where enrollment_id is not null;
create unique index if not exists uq_exam_standalone_user_attempt
    on exam_sessions(exam_id, user_id, attempt_no)
    where enrollment_id is null;
create index if not exists idx_exam_session_enrollment
    on exam_sessions(enrollment_id)
    where enrollment_id is not null;
