-- DEVELOPMENT / DEMO ONLY (dev and test profiles). Fictional customers — any resemblance to real people is coincidental.
-- Emails use the reserved example.com domain; phone numbers are made up.
-- Named "z1" so it runs after R__dev_seed_users.sql (Flyway runs repeatable migrations in name order).
-- Idempotent: existing rows (matched on the unique phone) are left untouched, so your edits survive re-runs.
-- created_at is relative to the first run, spreading customers over the last ~6 months for the charts.

INSERT INTO customers (full_name, email, phone, occupation, employment_type, income_range, city, segment, status,
                       branch_id, assigned_officer_id, created_by, created_at, updated_at, archived_at) VALUES
    ('Rafiqul Islam', 'rafiqul.islam1@example.com', '+8801700012345', 'Bank Employee', 'SALARIED', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 DAY), NULL),
    ('Shirin Akter', 'shirin.akter2@example.com', '+8801800020264', 'School Teacher', 'SALARIED', 'FROM_30K_TO_60K', 'Dhaka', 'MASS', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 140 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 140 DAY), NULL),
    ('Mahmudul Hasan', 'mahmudul.hasan3@example.com', '+8801900028183', 'Software Engineer', 'SALARIED', 'FROM_100K_TO_200K', 'Dhaka', 'AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 DAY), NULL),
    ('Taslima Begum', 'taslima.begum4@example.com', '+8801500036102', 'Doctor', 'PROFESSIONAL', 'ABOVE_200K', 'Dhaka', 'AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 95 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 95 DAY), NULL),
    ('Arif Chowdhury', 'arif.chowdhury5@example.com', '+8801600044021', 'Garments Merchandiser', 'SALARIED', 'FROM_60K_TO_100K', 'Narayanganj', 'EMERGING_AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 DAY), NULL),
    ('Nasrin Sultana', 'nasrin.sultana6@example.com', '+8801300051940', 'Pharmacist', 'PROFESSIONAL', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 34 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 34 DAY), NULL),
    ('Kamrul Hasan', 'kamrul.hasan7@example.com', '+8801700059859', 'University Student', 'STUDENT', 'BELOW_30K', 'Dhaka', 'MASS', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 DAY), NULL),
    ('Sabina Yasmin', 'sabina.yasmin8@example.com', '+8801800067778', 'Architect', 'PROFESSIONAL', 'FROM_100K_TO_200K', 'Dhaka', 'AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'), (SELECT id FROM users WHERE email = 'nusrat.jahan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), NULL),
    ('Habibur Rahman', 'habibur.rahman9@example.com', '+8801900075697', 'Retired Civil Servant', 'RETIRED', 'FROM_30K_TO_60K', 'Dhaka', 'MASS', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 165 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 165 DAY), NULL),
    ('Moushumi Das', 'moushumi.das10@example.com', '+8801500083616', 'Fashion Designer', 'SELF_EMPLOYED', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 110 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 110 DAY), NULL),
    ('Jahangir Alam', 'jahangir.alam11@example.com', '+8801600091535', 'Pharmacy Owner', 'BUSINESS_OWNER', 'FROM_100K_TO_200K', 'Gazipur', 'SME', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 80 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 80 DAY), NULL),
    ('Rumana Ferdous', 'rumana.ferdous12@example.com', '+8801300099454', 'Bank Employee', 'SALARIED', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'INACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY), NULL),
    ('Sohel Rana', 'sohel.rana13@example.com', '+8801700107373', 'Delivery Rider', 'SALARIED', 'BELOW_30K', 'Dhaka', 'MASS', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 20 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 20 DAY), NULL),
    ('Farhana Haque', 'farhana.haque14@example.com', '+8801800115292', 'Chartered Accountant', 'PROFESSIONAL', 'ABOVE_200K', 'Dhaka', 'AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'), (SELECT id FROM users WHERE email = 'tanvir.ahmed@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), NULL),
    ('Abdul Karim', 'abdul.karim15@example.com', '+8801900123211', 'Wholesale Trader', 'BUSINESS_OWNER', 'ABOVE_200K', 'Dhaka', 'SME', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 130 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 130 DAY), NULL),
    ('Lubna Rahman', 'lubna.rahman16@example.com', '+8801500131130', 'NGO Program Officer', 'SALARIED', 'FROM_30K_TO_60K', 'Dhaka', 'MASS', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'), (SELECT id FROM users WHERE email = 'manager.motijheel@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 DAY), NULL),
    ('Monirul Islam', 'monirul.islam17@example.com', '+8801600139049', 'Freelance Developer', 'SELF_EMPLOYED', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), NULL, NULL,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 DAY), NULL),
    ('Rehana Parvin', 'rehana.parvin18@example.com', '+8801300146968', 'Homemaker', 'OTHER', 'BELOW_30K', 'Dhaka', 'MASS', 'ARCHIVED',
     (SELECT id FROM branches WHERE code = 'DHK-MTJ'), NULL, NULL,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 100 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 100 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 70 DAY)),
    ('Shafiqul Alam', 'shafiqul.alam19@example.com', '+8801700154887', 'Restaurant Owner', 'BUSINESS_OWNER', 'FROM_100K_TO_200K', 'Dhaka', 'SME', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 160 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 160 DAY), NULL),
    ('Nadia Islam', 'nadia.islam20@example.com', '+8801800162806', 'Marketing Manager', 'SALARIED', 'FROM_100K_TO_200K', 'Dhaka', 'AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 125 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 125 DAY), NULL),
    ('Tanjim Hossain', 'tanjim.hossain21@example.com', '+8801900170725', 'Lecturer', 'SALARIED', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 90 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 90 DAY), NULL),
    ('Sharmin Nahar', 'sharmin.nahar22@example.com', '+8801500178644', 'Dentist', 'PROFESSIONAL', 'ABOVE_200K', 'Dhaka', 'AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 75 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 75 DAY), NULL),
    ('Rashed Khan', 'rashed.khan23@example.com', '+8801600186563', 'IT Consultant', 'SELF_EMPLOYED', 'FROM_100K_TO_200K', 'Dhaka', 'AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 40 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 40 DAY), NULL),
    ('Mitu Chakma', 'mitu.chakma24@example.com', '+8801300194482', 'Nurse', 'SALARIED', 'FROM_30K_TO_60K', 'Dhaka', 'MASS', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 25 DAY), NULL),
    ('Ziaur Rahman', 'ziaur.rahman25@example.com', '+8801700202401', 'Import Agent', 'BUSINESS_OWNER', 'ABOVE_200K', 'Dhaka', 'SME', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY), NULL),
    ('Afroza Khatun', 'afroza.khatun26@example.com', '+8801800210320', 'Government Officer', 'SALARIED', 'FROM_60K_TO_100K', 'Dhaka', 'EMERGING_AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'), (SELECT id FROM users WHERE email = 'imran.hossain@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), NULL),
    ('Nazmul Huda', 'nazmul.huda27@example.com', '+8801900218239', 'Real Estate Broker', 'SELF_EMPLOYED', 'ABOVE_200K', 'Dhaka', 'AFFLUENT', 'ACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 145 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 145 DAY), NULL),
    ('Sumaiya Akter', 'sumaiya.akter28@example.com', '+8801500226158', 'Graphic Designer', 'SELF_EMPLOYED', 'FROM_30K_TO_60K', 'Dhaka', 'MASS', 'INACTIVE',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 55 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 55 DAY), NULL),
    ('Ehsanul Kabir', 'ehsanul.kabir29@example.com', '+8801600234077', 'Logistics Manager', 'SALARIED', 'FROM_100K_TO_200K', 'Dhaka', 'AFFLUENT', 'PROSPECT',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'), (SELECT id FROM users WHERE email = 'manager.gulshan@bankpro.demo'),
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY), NULL),
    ('Selina Hossain', 'selina.hossain30@example.com', '+8801300241996', 'Boutique Owner', 'BUSINESS_OWNER', 'FROM_60K_TO_100K', 'Dhaka', 'SME', 'ARCHIVED',
     (SELECT id FROM branches WHERE code = 'DHK-GUL'), NULL, NULL,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 135 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 135 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 105 DAY))
AS new_row
ON DUPLICATE KEY UPDATE id = customers.id;

-- Products these customers already hold (manually recorded relationships).
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801700012345'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801700012345'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801800020264'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801500036102'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801500036102'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801300051940'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801900075697'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801900075697'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'CUR-STD'
WHERE c.phone = '+8801600091535'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SME-SVC'
WHERE c.phone = '+8801600091535'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801300099454'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'CUR-STD'
WHERE c.phone = '+8801900123211'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'CUR-STD'
WHERE c.phone = '+8801700154887'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SME-SVC'
WHERE c.phone = '+8801700154887'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801800162806'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'CC-CLASSIC'
WHERE c.phone = '+8801800162806'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801500178644'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801500178644'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'DPS-STD'
WHERE c.phone = '+8801500178644'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801800210320'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801900218239'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'FDR-1Y'
WHERE c.phone = '+8801900218239'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
INSERT INTO customer_products (customer_id, product_id, since_date)
SELECT c.id, p.id, DATE(c.created_at) FROM customers c JOIN banking_products p ON p.code = 'SAV-STD'
WHERE c.phone = '+8801500226158'
  AND NOT EXISTS (SELECT 1 FROM customer_products cp WHERE cp.customer_id = c.id AND cp.product_id = p.id);
