ALTER TABLE interview_sessions
    ADD COLUMN IF NOT EXISTS application_document_id BIGINT NULL AFTER position_title;

ALTER TABLE interview_sessions
    ADD COLUMN IF NOT EXISTS application_document_snapshot TEXT NULL AFTER application_document_id;

ALTER TABLE interview_sessions
    MODIFY COLUMN application_document_snapshot TEXT NULL;

ALTER TABLE interview_sessions
    MODIFY COLUMN resume_snapshot TEXT NULL;

ALTER TABLE interview_sessions
    MODIFY COLUMN cover_letter_snapshot TEXT NULL;

ALTER TABLE interview_sessions
    MODIFY COLUMN job_posting_snapshot TEXT NULL;
