-- =====================================================================
-- BankPro V1 schema (MySQL 8.4)
-- All timestamps are stored in UTC. Money uses DECIMAL(15,2) (BDT).
-- Display codes such as CUS-000123 / LD-000045 are derived from ids,
-- so they are not stored (see DECISIONS.md D-14).
-- =====================================================================

CREATE TABLE branches (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_branches PRIMARY KEY (id),
    CONSTRAINT uk_branches_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    email          VARCHAR(190) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    role           VARCHAR(30)  NOT NULL,
    job_title      VARCHAR(80)  NULL,
    branch_id      BIGINT       NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')),
    -- Every non-admin must belong to a branch: this is what branch-scoped security relies on.
    CONSTRAINT ck_users_branch_required CHECK (role = 'ADMIN' OR branch_id IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_users_branch_role ON users (branch_id, role);

CREATE TABLE customers (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    full_name            VARCHAR(120) NOT NULL,
    email                VARCHAR(190) NULL,
    phone                VARCHAR(20)  NOT NULL,
    occupation           VARCHAR(80)  NULL,
    employment_type      VARCHAR(30)  NULL,
    income_range         VARCHAR(30)  NULL,
    city                 VARCHAR(80)  NULL,
    segment              VARCHAR(30)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PROSPECT',
    branch_id            BIGINT       NOT NULL,
    assigned_officer_id  BIGINT       NULL,
    last_interaction_at  DATETIME(6)  NULL,
    archived_at          DATETIME(6)  NULL,
    created_by           BIGINT       NULL,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT uk_customers_phone UNIQUE (phone),
    CONSTRAINT fk_customers_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_customers_officer FOREIGN KEY (assigned_officer_id) REFERENCES users (id),
    CONSTRAINT fk_customers_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_customers_segment CHECK (segment IN ('MASS', 'EMERGING_AFFLUENT', 'AFFLUENT', 'SME')),
    CONSTRAINT ck_customers_status CHECK (status IN ('PROSPECT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_customers_employment CHECK (employment_type IS NULL OR employment_type IN
        ('SALARIED', 'SELF_EMPLOYED', 'BUSINESS_OWNER', 'PROFESSIONAL', 'STUDENT', 'RETIRED', 'OTHER')),
    CONSTRAINT ck_customers_income CHECK (income_range IS NULL OR income_range IN
        ('BELOW_30K', 'FROM_30K_TO_60K', 'FROM_60K_TO_100K', 'FROM_100K_TO_200K', 'ABOVE_200K'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_customers_branch_status ON customers (branch_id, status);
CREATE INDEX ix_customers_officer ON customers (assigned_officer_id);
CREATE INDEX ix_customers_created_at ON customers (created_at);
CREATE INDEX ix_customers_full_name ON customers (full_name);

CREATE TABLE banking_products (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    code               VARCHAR(30)   NOT NULL,
    name               VARCHAR(120)  NOT NULL,
    category           VARCHAR(20)   NOT NULL,
    description        TEXT          NOT NULL,
    eligibility_notes  TEXT          NULL,
    benefits           JSON          NULL,
    -- Optional, clearly fictional, illustrative annual rate (%). Never a real offer.
    illustrative_rate  DECIMAL(5, 2) NULL,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_banking_products PRIMARY KEY (id),
    CONSTRAINT uk_banking_products_code UNIQUE (code),
    CONSTRAINT ck_banking_products_category CHECK (category IN ('ACCOUNT', 'DEPOSIT', 'CARD', 'LOAN', 'SME')),
    CONSTRAINT ck_banking_products_rate CHECK (illustrative_rate IS NULL OR (illustrative_rate >= 0 AND illustrative_rate <= 100))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE leads (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    customer_id          BIGINT         NOT NULL,
    product_id           BIGINT         NOT NULL,
    branch_id            BIGINT         NOT NULL,
    assigned_officer_id  BIGINT         NOT NULL,
    source               VARCHAR(30)    NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'NEW',
    priority             VARCHAR(10)    NOT NULL DEFAULT 'MEDIUM',
    estimated_value      DECIMAL(15, 2) NOT NULL DEFAULT 0,
    expected_close_date  DATE           NULL,
    loss_reason          VARCHAR(30)    NULL,
    loss_note            VARCHAR(500)   NULL,
    notes                TEXT           NULL,
    last_contacted_at    DATETIME(6)    NULL,
    status_changed_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by           BIGINT         NULL,
    created_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_leads PRIMARY KEY (id),
    CONSTRAINT fk_leads_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_leads_product FOREIGN KEY (product_id) REFERENCES banking_products (id),
    CONSTRAINT fk_leads_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_leads_officer FOREIGN KEY (assigned_officer_id) REFERENCES users (id),
    CONSTRAINT fk_leads_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_leads_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'CONVERTED', 'LOST')),
    CONSTRAINT ck_leads_source CHECK (source IN
        ('BRANCH_WALK_IN', 'REFERRAL', 'PHONE_CALL', 'WEBSITE', 'CAMPAIGN', 'EXISTING_CUSTOMER')),
    CONSTRAINT ck_leads_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_leads_value CHECK (estimated_value >= 0),
    CONSTRAINT ck_leads_loss_reason CHECK (loss_reason IS NULL OR loss_reason IN
        ('PRICE_OR_RATE', 'CHOSE_COMPETITOR', 'NOT_ELIGIBLE', 'NO_RESPONSE', 'NO_LONGER_INTERESTED', 'OTHER')),
    -- A LOST lead must say why.
    CONSTRAINT ck_leads_lost_has_reason CHECK (status <> 'LOST' OR loss_reason IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_leads_branch_status ON leads (branch_id, status);
CREATE INDEX ix_leads_officer_status ON leads (assigned_officer_id, status);
CREATE INDEX ix_leads_customer ON leads (customer_id);
CREATE INDEX ix_leads_product ON leads (product_id);
CREATE INDEX ix_leads_created_at ON leads (created_at);

CREATE TABLE sales (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    lead_id       BIGINT         NOT NULL,
    customer_id   BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    branch_id     BIGINT         NOT NULL,
    officer_id    BIGINT         NOT NULL,
    amount        DECIMAL(15, 2) NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    converted_at  DATETIME(6)    NOT NULL,
    decided_at    DATETIME(6)    NULL,
    decided_by    BIGINT         NULL,
    decision_note VARCHAR(500)   NULL,
    CONSTRAINT pk_sales PRIMARY KEY (id),
    -- The database itself guarantees that one lead produces at most one sale.
    CONSTRAINT uk_sales_lead UNIQUE (lead_id),
    CONSTRAINT fk_sales_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES banking_products (id),
    CONSTRAINT fk_sales_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_sales_officer FOREIGN KEY (officer_id) REFERENCES users (id),
    CONSTRAINT fk_sales_decided_by FOREIGN KEY (decided_by) REFERENCES users (id),
    CONSTRAINT ck_sales_amount CHECK (amount > 0),
    CONSTRAINT ck_sales_status CHECK (status IN ('COMPLETED', 'PENDING_APPROVAL', 'DECLINED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_sales_branch_status_date ON sales (branch_id, status, converted_at);
CREATE INDEX ix_sales_officer_status_date ON sales (officer_id, status, converted_at);
CREATE INDEX ix_sales_product ON sales (product_id);

CREATE TABLE customer_products (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    since_date      DATE   NOT NULL,
    source_sale_id  BIGINT NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_customer_products PRIMARY KEY (id),
    CONSTRAINT uk_customer_products_sale UNIQUE (source_sale_id),
    CONSTRAINT fk_customer_products_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_customer_products_product FOREIGN KEY (product_id) REFERENCES banking_products (id),
    CONSTRAINT fk_customer_products_sale FOREIGN KEY (source_sale_id) REFERENCES sales (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_customer_products_customer ON customer_products (customer_id);

CREATE TABLE follow_up_activities (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id          BIGINT       NOT NULL,
    lead_id              BIGINT       NULL,
    branch_id            BIGINT       NOT NULL,
    assigned_officer_id  BIGINT       NOT NULL,
    activity_type        VARCHAR(30)  NOT NULL,
    scheduled_at         DATETIME(6)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    notes                TEXT         NULL,
    outcome              VARCHAR(500) NULL,
    completed_at         DATETIME(6)  NULL,
    created_by           BIGINT       NULL,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_follow_up_activities PRIMARY KEY (id),
    CONSTRAINT fk_follow_ups_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_follow_ups_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_follow_ups_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_follow_ups_officer FOREIGN KEY (assigned_officer_id) REFERENCES users (id),
    CONSTRAINT fk_follow_ups_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_follow_ups_type CHECK (activity_type IN
        ('PHONE_CALL', 'BRANCH_MEETING', 'EMAIL', 'PRODUCT_DISCUSSION', 'DOCUMENT_COLLECTION', 'GENERAL_FOLLOW_UP')),
    CONSTRAINT ck_follow_ups_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_follow_ups_completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_follow_ups_officer_status_time ON follow_up_activities (assigned_officer_id, status, scheduled_at);
CREATE INDEX ix_follow_ups_branch_status_time ON follow_up_activities (branch_id, status, scheduled_at);
CREATE INDEX ix_follow_ups_customer ON follow_up_activities (customer_id);

CREATE TABLE customer_interactions (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id       BIGINT       NOT NULL,
    lead_id           BIGINT       NULL,
    follow_up_id      BIGINT       NULL,
    recorded_by       BIGINT       NOT NULL,
    interaction_type  VARCHAR(30)  NOT NULL,
    summary           TEXT         NOT NULL,
    occurred_at       DATETIME(6)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_customer_interactions PRIMARY KEY (id),
    CONSTRAINT uk_interactions_follow_up UNIQUE (follow_up_id),
    CONSTRAINT fk_interactions_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_interactions_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_interactions_follow_up FOREIGN KEY (follow_up_id) REFERENCES follow_up_activities (id),
    CONSTRAINT fk_interactions_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id),
    CONSTRAINT ck_interactions_type CHECK (interaction_type IN
        ('PHONE_CALL', 'BRANCH_MEETING', 'EMAIL', 'PRODUCT_DISCUSSION', 'DOCUMENT_COLLECTION', 'GENERAL_FOLLOW_UP', 'NOTE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_interactions_customer_time ON customer_interactions (customer_id, occurred_at);

CREATE TABLE sales_targets (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT         NOT NULL,
    user_id       BIGINT         NULL,      -- NULL = branch-level target
    period_type   VARCHAR(10)    NOT NULL,
    period_start  DATE           NOT NULL,
    period_end    DATE           NOT NULL,
    category      VARCHAR(30)    NOT NULL,
    target_value  DECIMAL(15, 2) NOT NULL,
    created_by    BIGINT         NOT NULL,
    created_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- MySQL UNIQUE treats NULLs as different values, so map "branch target" to 0 for the uniqueness check.
    owner_key     BIGINT AS (IFNULL(user_id, 0)) STORED,
    CONSTRAINT pk_sales_targets PRIMARY KEY (id),
    CONSTRAINT uk_sales_targets UNIQUE (branch_id, owner_key, period_type, period_start, category),
    CONSTRAINT fk_targets_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_targets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_targets_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_targets_period_type CHECK (period_type IN ('MONTHLY', 'QUARTERLY')),
    CONSTRAINT ck_targets_period_range CHECK (period_end >= period_start),
    CONSTRAINT ck_targets_category CHECK (category IN
        ('NEW_CUSTOMERS', 'DEPOSIT_SALES', 'CREDIT_CARD_SALES', 'LOAN_LEADS', 'CONVERTED_LEADS')),
    CONSTRAINT ck_targets_value CHECK (target_value > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_targets_period ON sales_targets (period_start, period_end);

CREATE TABLE notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(40)  NOT NULL,
    message     VARCHAR(300) NOT NULL,
    link        VARCHAR(200) NULL,
    -- Prevents creating the same reminder twice, e.g. 'FOLLOW_UP_OVERDUE:42'.
    dedupe_key  VARCHAR(100) NULL,
    read_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uk_notifications_dedupe UNIQUE (user_id, dedupe_key),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_notifications_user_read ON notifications (user_id, read_flag, created_at);

CREATE TABLE audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id     BIGINT       NULL,      -- NULL for e.g. failed logins with an unknown email
    action       VARCHAR(50)  NOT NULL,
    entity_type  VARCHAR(40)  NULL,
    entity_id    BIGINT       NULL,
    details      JSON         NULL,      -- never secrets or unnecessary personal data
    ip_address   VARCHAR(45)  NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
