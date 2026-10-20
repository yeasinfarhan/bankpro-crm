-- DEVELOPMENT / DEMO ONLY. Fictional leads and SIMULATED sales (no money, no real accounts).
-- Runs after the customer seed ("z2" > "z1"). Idempotent: a lead is only inserted if that customer has
-- no lead for the same product yet; sales/holdings use NOT EXISTS / the UNIQUE(lead_id) rule.

INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'EXISTING_CUSTOMER', 'PROPOSAL', 'HIGH', 500000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 25 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 20 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801700012345'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'BRANCH_WALK_IN', 'CONVERTED', 'MEDIUM', 120000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 100 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 100 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 110 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 100 DAY)
FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801800020264'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 120000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 100 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801800020264' AND p.code = 'DPS-STD' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'WEBSITE', 'QUALIFIED', 'MEDIUM', 300000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 15 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY)
FROM customers c JOIN banking_products p ON p.code = 'CC-CLASSIC'
WHERE c.phone = '+8801900028183'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'REFERRAL', 'PROPOSAL', 'HIGH', 6000000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 40 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY)
FROM customers c JOIN banking_products p ON p.code = 'HL-STD'
WHERE c.phone = '+8801500036102'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'PHONE_CALL', 'CONTACTED', 'MEDIUM', 500000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 DAY)
FROM customers c JOIN banking_products p ON p.code = 'PL-STD'
WHERE c.phone = '+8801600044021'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'EXISTING_CUSTOMER', 'CONVERTED', 'HIGH', 300000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 32 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801300051940'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 300000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801300051940' AND p.code = 'FDR-1Y' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'CAMPAIGN', 'NEW', 'LOW', 10000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 43 DAY)), NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801700059859'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'WEBSITE', 'NEW', 'MEDIUM', 200000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 44 DAY)), NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY)
FROM customers c JOIN banking_products p ON p.code = 'CC-CLASSIC'
WHERE c.phone = '+8801800067778'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'EXISTING_CUSTOMER', 'CONVERTED', 'MEDIUM', 240000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY)
FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801500036102'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 240000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801500036102' AND p.code = 'DPS-STD' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'BRANCH_WALK_IN', 'CONVERTED', 'HIGH', 1000000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 158 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801900075697'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 1000000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801900075697' AND p.code = 'FDR-1Y' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'CAMPAIGN', 'LOST', 'LOW', 15000,
       NULL, 'NO_RESPONSE', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 80 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 80 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 95 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 80 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801500083616'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'REFERRAL', 'CONVERTED', 'HIGH', 2000000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 78 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SME-SVC'
WHERE c.phone = '+8801600091535'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 2000000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801600091535' AND p.code = 'SME-SVC' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'EXISTING_CUSTOMER', 'CONVERTED', 'HIGH', 1500000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY)
FROM customers c JOIN banking_products p ON p.code = 'PL-STD'
WHERE c.phone = '+8801600091535'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 1500000, 'PENDING_APPROVAL', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801600091535' AND p.code = 'PL-STD' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'PHONE_CALL', 'LOST', 'MEDIUM', 150000,
       NULL, 'CHOSE_COMPETITOR', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 50 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 50 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 50 DAY)
FROM customers c JOIN banking_products p ON p.code = 'CC-CLASSIC'
WHERE c.phone = '+8801300099454'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'BRANCH_WALK_IN', 'QUALIFIED', 'LOW', 5000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 35 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 10 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801700107373'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'REFERRAL', 'CONTACTED', 'HIGH', 800000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 40 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801800115292'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), 'EXISTING_CUSTOMER', 'PROPOSAL', 'HIGH', 3000000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 20 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SME-SVC'
WHERE c.phone = '+8801900123211'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), 'CAMPAIGN', 'NEW', 'MEDIUM', 60000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 39 DAY)), NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY)
FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801500131130'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), 'WEBSITE', 'CONTACTED', 'LOW', 20000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 37 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 DAY), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801600139049'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'EXISTING_CUSTOMER', 'CONVERTED', 'HIGH', 1000000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 55 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY)
FROM customers c JOIN banking_products p ON p.code = 'PL-STD'
WHERE c.phone = '+8801700154887'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 1000000, 'DECLINED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 43 DAY), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'Simulated decision'
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801700154887' AND p.code = 'PL-STD' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'EXISTING_CUSTOMER', 'CONVERTED', 'HIGH', 600000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 38 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801800162806'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 600000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801800162806' AND p.code = 'FDR-1Y' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'BRANCH_WALK_IN', 'QUALIFIED', 'MEDIUM', 100000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 33 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY)
FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801900170725'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'REFERRAL', 'CONTACTED', 'HIGH', 8000000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 27 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 11 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 11 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 11 DAY)
FROM customers c JOIN banking_products p ON p.code = 'HL-STD'
WHERE c.phone = '+8801500178644'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'WEBSITE', 'CONVERTED', 'MEDIUM', 400000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY)
FROM customers c JOIN banking_products p ON p.code = 'CC-CLASSIC'
WHERE c.phone = '+8801600186563'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 400000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 13 DAY), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'Simulated decision'
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801600186563' AND p.code = 'CC-CLASSIC' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'CAMPAIGN', 'NEW', 'LOW', 5000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 42 DAY)), NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801300194482'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'REFERRAL', 'PROPOSAL', 'HIGH', 2500000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 38 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY)
FROM customers c JOIN banking_products p ON p.code = 'SME-SVC'
WHERE c.phone = '+8801700202401'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'BRANCH_WALK_IN', 'CONVERTED', 'MEDIUM', 150000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY)
FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801800210320'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 150000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801800210320' AND p.code = 'DPS-STD' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'EXISTING_CUSTOMER', 'CONVERTED', 'HIGH', 2000000,
       NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 DAY), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 128 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801900218239'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO sales (lead_id, customer_id, product_id, branch_id, officer_id, amount, status, converted_at, decided_at, decided_by, decision_note)
SELECT l.id, l.customer_id, l.product_id, l.branch_id, l.assigned_officer_id, 2000000, 'COMPLETED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 DAY), NULL, NULL, NULL
FROM leads l JOIN customers c ON c.id = l.customer_id JOIN banking_products p ON p.id = l.product_id
WHERE c.phone = '+8801900218239' AND p.code = 'FDR-1Y' AND l.status = 'CONVERTED'
  AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.lead_id = l.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'PHONE_CALL', 'LOST', 'LOW', 100000,
       NULL, 'NOT_ELIGIBLE', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 40 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 40 DAY), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 40 DAY)
FROM customers c JOIN banking_products p ON p.code = 'CC-CLASSIC'
WHERE c.phone = '+8801500226158'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
INSERT INTO leads (customer_id, product_id, branch_id, assigned_officer_id, source, status, priority,
                   estimated_value, expected_close_date, loss_reason, last_contacted_at, status_changed_at, created_by, created_at, updated_at)
SELECT c.id, p.id, c.branch_id, (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'REFERRAL', 'QUALIFIED', 'HIGH', 1200000,
       DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 36 DAY)), NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY)
FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801600234077'
  AND NOT EXISTS (SELECT 1 FROM leads x WHERE x.customer_id = c.id AND x.product_id = p.id);
-- Completed simulated sales add the product to the customer's holdings and make prospects active.
INSERT INTO customer_products (customer_id, product_id, since_date, source_sale_id)
SELECT s.customer_id, s.product_id, DATE(s.converted_at), s.id FROM sales s
WHERE s.status = 'COMPLETED' AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.source_sale_id = s.id);

UPDATE customers c SET c.status = 'ACTIVE'
WHERE c.status = 'PROSPECT' AND EXISTS (SELECT 1 FROM sales s WHERE s.customer_id = c.id AND s.status = 'COMPLETED');

