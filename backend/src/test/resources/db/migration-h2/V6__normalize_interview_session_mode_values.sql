UPDATE interview_sessions
SET mode = 'COMPREHENSIVE'
WHERE mode IS NULL OR TRIM(mode) = '';
