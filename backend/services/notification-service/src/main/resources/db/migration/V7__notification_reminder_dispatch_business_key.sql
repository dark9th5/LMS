ALTER TABLE notification_reminder_dispatches ADD COLUMN IF NOT EXISTS business_key varchar(180) NOT NULL DEFAULT '';
