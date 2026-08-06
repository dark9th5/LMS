ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS attempt_no integer NOT NULL DEFAULT 1;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS text_answer text;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS file_ids_json text NOT NULL DEFAULT '[]';
