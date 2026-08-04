create index if not exists idx_report_due_incomplete
    on learner_course_read_model(due_at asc)
    where completed = false and due_at is not null;
