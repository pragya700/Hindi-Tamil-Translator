-- Run in MySQL Workbench or the mysql command line.
CREATE DATABASE IF NOT EXISTS hindi_tamil_translator
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE hindi_tamil_translator;
SHOW TABLES;

-- ---- After you register a user, run these to verify (table is created by Hibernate) ----
-- SELECT id, vit_id, name, email, password FROM users;
--   -> the password column must show a hash starting with $2a$ (BCrypt), never plain text.
-- SHOW CREATE TABLE users;
--   -> shows the UNIQUE keys uk_users_email and uk_users_vit_id.
