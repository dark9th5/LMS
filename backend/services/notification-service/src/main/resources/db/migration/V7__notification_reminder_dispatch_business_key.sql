ALTER TABLE notification_reminder_dispatches ADD COLUMN IF NOT EXISTS business_key varchar(200);
ALTER TABLE notification_reminder_rules ADD COLUMN IF NOT EXISTS days_before_due integer DEFAULT 1;
