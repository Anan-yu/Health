-- V31: rotate the preconfigured platform administrator credentials.
-- The password is stored only as a BCrypt digest; the clear-text value is
-- intentionally not kept in the repository or emitted to application logs.

UPDATE sys_user
SET username = 'admin',
    password_hash = '$2b$12$jDVZcHdilvVRHaOfG8RVnOO/wj04mkTm8ycI0wOowXqudU2Zat91.',
    updated_by = 10001,
    updated_at = NOW()
WHERE id = 10001
  AND deleted = 0;
