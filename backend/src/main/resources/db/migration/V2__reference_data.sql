-- Reference data needed in every environment.
-- Bank, branches and products are FICTIONAL. Rates are illustrative sample values, not offers.

INSERT INTO branches (code, name, city) VALUES
    ('DHK-MTJ', 'Motijheel Branch', 'Dhaka'),
    ('DHK-GUL', 'Gulshan Branch', 'Dhaka'),
    ('CTG-AGR', 'Agrabad Branch', 'Chattogram');

INSERT INTO banking_products (code, name, category, description, eligibility_notes, benefits, illustrative_rate) VALUES
    ('SAV-STD', 'BankPro Savings Account', 'ACCOUNT',
     'Everyday savings account with debit card and internet banking. Fictional sample product.',
     'Illustrative only: individual customers with valid identification.',
     JSON_ARRAY('Free debit card', 'Internet & app banking', 'Monthly e-statement'), 3.00),
    ('CUR-STD', 'BankPro Current Account', 'ACCOUNT',
     'Transactional account for frequent payments and cheque usage. Fictional sample product.',
     'Illustrative only: individuals and businesses.',
     JSON_ARRAY('Cheque book', 'Unlimited transactions', 'Online fund transfer'), NULL),
    ('FDR-1Y', 'BankPro Fixed Deposit (FDR)', 'DEPOSIT',
     'Lump-sum term deposit for a fixed tenure. Fictional sample product.',
     'Illustrative only: minimum deposit and tenure set by the fictional bank.',
     JSON_ARRAY('Choice of tenure', 'Auto-renewal option', 'Loan against deposit'), 8.50),
    ('DPS-STD', 'BankPro Deposit Pension Scheme (DPS)', 'DEPOSIT',
     'Monthly instalment savings scheme for long-term goals. Fictional sample product.',
     'Illustrative only: monthly instalment for a 3–10 year term.',
     JSON_ARRAY('Small monthly instalments', 'Goal-based saving', 'Maturity lump sum'), 9.00),
    ('CC-CLASSIC', 'BankPro Classic Credit Card', 'CARD',
     'Credit card for everyday spending. Fictional sample product.',
     'Illustrative only: subject to the fictional bank''s credit assessment. BankPro does not assess eligibility.',
     JSON_ARRAY('Interest-free period', 'EMI on purchases', 'Reward points'), NULL),
    ('PL-STD', 'BankPro Personal Loan', 'LOAN',
     'Unsecured loan for personal needs. Fictional sample product.',
     'Illustrative only: subject to the fictional bank''s credit assessment. BankPro does not assess eligibility.',
     JSON_ARRAY('Flexible tenure', 'Fixed monthly instalments'), NULL),
    ('HL-STD', 'BankPro Home Loan', 'LOAN',
     'Loan to buy, build or renovate a home. Fictional sample product.',
     'Illustrative only: subject to the fictional bank''s credit and property assessment.',
     JSON_ARRAY('Long tenure', 'Step-up repayment option'), NULL),
    ('SME-SVC', 'BankPro SME Banking Services', 'SME',
     'Accounts, cash management and trade services for small businesses. Fictional sample product.',
     'Illustrative only: registered small and medium enterprises.',
     JSON_ARRAY('Dedicated relationship officer', 'Cash management', 'Trade services'), NULL);
