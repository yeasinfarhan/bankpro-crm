-- DEVELOPMENT / DEMO ONLY. Follow-ups (overdue, today, upcoming, completed) and interaction history.
-- Times are relative to the first run. Runs after the lead seed ("z3"). Idempotent via NOT EXISTS on the notes text.

INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'FDR-1Y' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'PHONE_CALL', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -20 HOUR), 'SCHEDULED', 'Call to confirm FDR tenure and amount', NULL, NULL, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801700012345'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Call to confirm FDR tenure and amount');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'HL-STD' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'BRANCH_MEETING', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 3 HOUR), 'SCHEDULED', 'Meet to review home loan proposal (fictional)', NULL, NULL, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801500036102'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Meet to review home loan proposal (fictional)');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'CC-CLASSIC' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'DOCUMENT_COLLECTION', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 50 HOUR), 'SCHEDULED', 'Collect salary certificate copy', NULL, NULL, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801900028183'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Collect salary certificate copy');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'PL-STD' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'EMAIL', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 120 HOUR), 'SCHEDULED', 'Send personal loan information sheet', NULL, NULL, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801600044021'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Send personal loan information sheet');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'FDR-1Y' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'PHONE_CALL', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -240 HOUR), 'COMPLETED', 'Discuss FDR options', 'Customer agreed to open a 1-year FDR', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -240 HOUR), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801300051940'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Discuss FDR options');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'FDR-1Y' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'PHONE_CALL', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -46 HOUR), 'SCHEDULED', 'Follow up on FDR referral', NULL, NULL, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801800115292'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Follow up on FDR referral');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'SAV-STD' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'PRODUCT_DISCUSSION', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 1 HOUR), 'SCHEDULED', 'Explain savings account features', NULL, NULL, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801700107373'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Explain savings account features');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'PL-STD' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'BRANCH_MEETING', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -480 HOUR), 'COMPLETED', 'Loan documents meeting', 'Documents received; application submitted for simulated approval', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -480 HOUR), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801600091535'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Loan documents meeting');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'SME-SVC' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), 'BRANCH_MEETING', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 26 HOUR), 'SCHEDULED', 'SME cash management proposal meeting', NULL, NULL, (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801900123211'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'SME cash management proposal meeting');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'HL-STD' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'PHONE_CALL', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -22 HOUR), 'SCHEDULED', 'Call about home loan property details', NULL, NULL, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801500178644'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Call about home loan property details');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'DPS-STD' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'PRODUCT_DISCUSSION', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 2 HOUR), 'SCHEDULED', 'Explain DPS instalment options', NULL, NULL, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801900170725'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Explain DPS instalment options');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'SME-SVC' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'BRANCH_MEETING', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 96 HOUR), 'SCHEDULED', 'Present SME services proposal', NULL, NULL, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801700202401'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Present SME services proposal');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'FDR-1Y' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'PHONE_CALL', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -120 HOUR), 'COMPLETED', 'Confirm FDR opening', 'FDR opened (simulated); customer satisfied', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL -120 HOUR), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801800162806'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Confirm FDR opening');
INSERT INTO follow_up_activities (customer_id, lead_id, branch_id, assigned_officer_id, activity_type, scheduled_at,
                                  status, notes, outcome, completed_at, created_by, created_at, updated_at)
SELECT c.id, (SELECT l.id FROM leads l JOIN banking_products p ON p.id = l.product_id WHERE l.customer_id = c.id AND p.code = 'FDR-1Y' LIMIT 1), c.branch_id, (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'BRANCH_MEETING', DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 50 HOUR), 'SCHEDULED', 'Discuss FDR amount with customer', NULL, NULL, (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), UTC_TIMESTAMP(6)
FROM customers c WHERE c.phone = '+8801600234077'
  AND NOT EXISTS (SELECT 1 FROM follow_up_activities f WHERE f.customer_id = c.id AND f.notes = 'Discuss FDR amount with customer');
-- Completed follow-ups appear in the customer's interaction history.
INSERT INTO customer_interactions (customer_id, lead_id, follow_up_id, recorded_by, interaction_type, summary, occurred_at)
SELECT f.customer_id, f.lead_id, f.id, f.assigned_officer_id, f.activity_type, CONCAT(f.outcome, ' — ', f.notes), f.completed_at
FROM follow_up_activities f
WHERE f.status = 'COMPLETED' AND NOT EXISTS (SELECT 1 FROM customer_interactions i WHERE i.follow_up_id = f.id);

INSERT INTO customer_interactions (customer_id, recorded_by, interaction_type, summary, occurred_at)
SELECT c.id, (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), 'NOTE', 'Customer asked about increasing DPS instalment next year', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 DAY) FROM customers c WHERE c.phone = '+8801800020264'
  AND NOT EXISTS (SELECT 1 FROM customer_interactions i WHERE i.customer_id = c.id AND i.summary = 'Customer asked about increasing DPS instalment next year');
INSERT INTO customer_interactions (customer_id, recorded_by, interaction_type, summary, occurred_at)
SELECT c.id, (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), 'BRANCH_MEETING', 'Walk-in visit: FDR maturity options discussed', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 140 DAY) FROM customers c WHERE c.phone = '+8801900075697'
  AND NOT EXISTS (SELECT 1 FROM customer_interactions i WHERE i.customer_id = c.id AND i.summary = 'Walk-in visit: FDR maturity options discussed');
INSERT INTO customer_interactions (customer_id, recorded_by, interaction_type, summary, occurred_at)
SELECT c.id, (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), 'PHONE_CALL', 'Card delivered (simulated); customer confirmed activation', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 DAY) FROM customers c WHERE c.phone = '+8801600186563'
  AND NOT EXISTS (SELECT 1 FROM customer_interactions i WHERE i.customer_id = c.id AND i.summary = 'Card delivered (simulated); customer confirmed activation');
INSERT INTO customer_interactions (customer_id, recorded_by, interaction_type, summary, occurred_at)
SELECT c.id, (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), 'EMAIL', 'Sent annual relationship summary (fictional)', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 20 DAY) FROM customers c WHERE c.phone = '+8801900218239'
  AND NOT EXISTS (SELECT 1 FROM customer_interactions i WHERE i.customer_id = c.id AND i.summary = 'Sent annual relationship summary (fictional)');
-- Keep "last interaction" in sync with the history above.
UPDATE customers c
JOIN (SELECT customer_id, MAX(occurred_at) AS last_at FROM customer_interactions GROUP BY customer_id) x ON x.customer_id = c.id
SET c.last_interaction_at = x.last_at
WHERE c.last_interaction_at IS NULL OR c.last_interaction_at < x.last_at;

