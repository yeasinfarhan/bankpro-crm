-- DEVELOPMENT / DEMO ONLY. Fictional sales targets for the current month, the previous month and the current
-- quarter (Bangladesh time), relative to the first run. Idempotent via the UNIQUE target key.

SET @month_start = DATE(DATE_FORMAT(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 6 HOUR), '%Y-%m-01'));
SET @prev_start  = DATE_SUB(@month_start, INTERVAL 1 MONTH);
SET @q_start     = MAKEDATE(YEAR(@month_start), 1) + INTERVAL (QUARTER(@month_start) - 1) QUARTER;

INSERT INTO sales_targets (branch_id, user_id, period_type, period_start, period_end, category, target_value, created_by)
SELECT u.branch_id, u.id, 'MONTHLY', p.start_date, LAST_DAY(p.start_date), t.category, t.value,
       (SELECT m.id FROM users m WHERE m.branch_id = u.branch_id AND m.role = 'BRANCH_MANAGER' LIMIT 1)
FROM users u
JOIN (SELECT @month_start AS start_date UNION ALL SELECT @prev_start) p
JOIN (SELECT 'DEPOSIT_SALES' AS category, 800000 AS value
      UNION ALL SELECT 'NEW_CUSTOMERS', 4
      UNION ALL SELECT 'CONVERTED_LEADS', 3
      UNION ALL SELECT 'CREDIT_CARD_SALES', 2) t
WHERE u.email IN ('nusrat.jahan@bankpro.demo', 'tanvir.ahmed@bankpro.demo', 'imran.hossain@bankpro.demo')
ON DUPLICATE KEY UPDATE id = sales_targets.id;

-- Branch-level targets for the current quarter
INSERT INTO sales_targets (branch_id, user_id, period_type, period_start, period_end, category, target_value, created_by)
SELECT b.id, NULL, 'QUARTERLY', @q_start, LAST_DAY(@q_start + INTERVAL 2 MONTH), t.category, t.value,
       (SELECT m.id FROM users m WHERE m.branch_id = b.id AND m.role = 'BRANCH_MANAGER' LIMIT 1)
FROM branches b
JOIN (SELECT 'DEPOSIT_SALES' AS category, 5000000 AS value
      UNION ALL SELECT 'NEW_CUSTOMERS', 20
      UNION ALL SELECT 'LOAN_LEADS', 6
      UNION ALL SELECT 'CONVERTED_LEADS', 12) t
WHERE b.code IN ('DHK-MTJ', 'DHK-GUL')
ON DUPLICATE KEY UPDATE id = sales_targets.id;
