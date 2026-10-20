-- DEVELOPMENT / DEMO ONLY (loaded by the dev profile, never in prod).
-- Fictional employees. Every demo account uses the password: BankPro@2026
-- Repeatable migration: re-runs when this file changes; ON DUPLICATE KEY keeps it idempotent.

INSERT INTO users (email, password_hash, full_name, role, job_title, branch_id, active) VALUES
    ('admin@bankpro.demo', '$2a$10$M6wT6vWR2IzLi/AD0d1wreo7hsSOLSgYLmIEL0bc0oJON2aBMCz8G',
     'Ayesha Rahman', 'ADMIN', 'System Administrator', NULL, TRUE),
    ('manager.motijheel@bankpro.demo', '$2a$10$7JthjJWfdn3ElPXlUZlZYOl8ZQADx5N7XxFa7SyP9EXl76iderjVu',
     'Kamal Hossain', 'BRANCH_MANAGER', 'Branch Manager', (SELECT id FROM branches WHERE code = 'DHK-MTJ'), TRUE),
    ('nusrat.jahan@bankpro.demo', '$2a$10$eDx//DRJNvcwy4RkdqOF8.xUJXRIe9/ICIHXAiy/xIUJe2f3Rbofi',
     'Nusrat Jahan', 'RELATIONSHIP_OFFICER', 'Relationship Officer', (SELECT id FROM branches WHERE code = 'DHK-MTJ'), TRUE),
    ('tanvir.ahmed@bankpro.demo', '$2a$10$7oCosYQ1A34p4yBghhcA/..fb8jLGsSd.caR2ckDrMea6frlfh4UG',
     'Tanvir Ahmed', 'RELATIONSHIP_OFFICER', 'Sales Officer', (SELECT id FROM branches WHERE code = 'DHK-MTJ'), TRUE),
    ('manager.gulshan@bankpro.demo', '$2a$10$q0WvFyKsgu3.CSvni3hzwujbBlFu3SME6J5Lf7Oldc5HakJ5MeRm6',
     'Farzana Akter', 'BRANCH_MANAGER', 'Branch Manager', (SELECT id FROM branches WHERE code = 'DHK-GUL'), TRUE),
    ('imran.hossain@bankpro.demo', '$2a$10$Fi5RuNIpe1NTzpV9X2M9fek0FiaV.cC8lqFI5naF0wilbKeR6gZKm',
     'Imran Hossain', 'RELATIONSHIP_OFFICER', 'Relationship Officer', (SELECT id FROM branches WHERE code = 'DHK-GUL'), TRUE),
    ('former.officer@bankpro.demo', '$2a$10$c9Ivjrd3sygDrZUhfTYFq.W58cMtKA1TQ9kRidTQ3c5e5M3BgiG3.',
     'Sabbir Khan', 'RELATIONSHIP_OFFICER', 'Relationship Officer (inactive)', (SELECT id FROM branches WHERE code = 'DHK-MTJ'), FALSE)
AS new_row
ON DUPLICATE KEY UPDATE
    password_hash = new_row.password_hash,
    full_name     = new_row.full_name,
    role          = new_row.role,
    job_title     = new_row.job_title,
    branch_id     = new_row.branch_id,
    active        = new_row.active;
