# Architecture & Design Decisions

Each entry: the decision, why, and what we gave up. Newest at the bottom.

### D-01 — Monorepo with `backend/`, `frontend/`, `docs/`
One Git repository is easier to review in a portfolio and keeps API and UI changes in the same commit.

### D-02 — Spring Boot 3.5.16, compiled for Java 21
The brief requires Spring Boot 3.x and Java 21. The code is compiled with `<java.version>21</java.version>`
(`--release 21`), so it runs on any JDK 21+. Your PC has JDK 25, which Spring Boot 3.5 supports and can build
this project; installing Temurin 21 is still recommended so local builds match production.

### D-03 — MySQL 8.4 LTS, schema owned by Flyway
MySQL 8.0 reached end of life in April 2026; 8.4 is the current 8.x LTS. Hibernate runs with
`ddl-auto=validate` — it checks entities against the schema but never changes it. Only Flyway migrations
change the schema, so every environment is reproducible.

### D-04 — JWT via Spring Security's OAuth2 Resource Server (HS256)
Uses `spring-boot-starter-oauth2-resource-server` with `NimbusJwtEncoder/Decoder` and a secret from
`JWT_SECRET`, instead of a hand-written JWT filter. Access token lifetime 60 minutes; no refresh token in the MVP.

### D-05 — Token stored in memory + `sessionStorage` on the frontend
Survives a page refresh, cleared when the tab closes, never in `localStorage`. Any JS-readable token is exposed
to XSS, so production uses a strict CSP and never renders untrusted HTML. httpOnly cookies = future improvement.

### D-06 — Money as `DECIMAL(15,2)` / `BigDecimal`, currency BDT

### D-07 — "Sales Officer" uses the RELATIONSHIP_OFFICER role (`users.job_title` distinguishes them)

### D-08 — Branch scope enforced in the service layer; out-of-scope = 404
Queries are built from a `DataScope` derived from the logged-in user, never from request input.

### D-09 — Relationship Officers read all customers in their branch but edit only their own
Leads and follow-ups are visible to the owning officer only.

### D-10 — Loan and card conversions create `PENDING_APPROVAL` sales
Only `COMPLETED` sales count toward sales value; a manager records the simulated decision.

### D-11 — Conversion uses pessimistic locking + `UNIQUE(sales.lead_id)` and is idempotent

### D-12 — Store UTC, compute business periods in Asia/Dhaka (one injectable `Clock`)

### D-13 — Soft delete for customers, deactivate for products, no deletes for leads/sales/audit

### D-14 — Derived figures are never stored
Includes KPIs, achievement %, pipeline value **and display codes**: `CUS-000123` / `LD-000045` are formatted
from the numeric id in the API layer instead of being stored columns.

### D-15 — Integration tests use Testcontainers MySQL 8.4
Docker Engine 29 requires Testcontainers ≥ 1.21.4, pinned in `pom.xml`.

### D-16 — Build environment constraint
The assistant's sandboxes cannot reach Maven Central, so backend compilation and tests run on the developer's PC.
Frontend lint/tests/build are verified in the sandbox. Nothing is reported as passing without seeing the output.

### D-17 — Spring Boot 3.5 is past open-source support
3.5.16 (June 2026) is the final OSS 3.x release; OSS support ended 2026-06-30. We follow the brief (3.x) because
the vast majority of learning material targets Boot 3. Migrating to Spring Boot 4 is listed as future work.

### D-18 — React Router 7 instead of 8
React Router 8 requires Node ≥ 22.22; the PC has Node 22.17. v7 has the same component API used here
(`BrowserRouter`, `Routes`, `Route`) and far more tutorials.

### D-19 — Frontend toolchain: Vite 8, Tailwind CSS 4, Vitest 5, oxlint
Tailwind 4 defines design tokens in CSS (`@theme` in `src/index.css`). oxlint is the linter shipped by the
current Vite template (fast, ESLint-compatible rules incl. React hooks rules).

### D-20 — Vite dev proxy for `/api`
In development the browser calls `/api` on port 5173 and Vite forwards to `localhost:8080`, so there are no
CORS issues locally. Production uses `VITE_API_BASE_URL` plus the backend's explicit CORS allow-list.

### D-21 — Dev profile may run without `JWT_SECRET`; prod refuses to start
If `JWT_SECRET` is empty in dev, a random key is generated per run (logged as a warning) — everyone is signed
out on restart. Outside dev, a missing or short (< 32 bytes) secret stops the application at startup.

### D-22 — Tokens are not revoked before expiry
A JWT stays valid until it expires (60 min) even if the user is deactivated meanwhile. `/api/auth/me` re-reads the
user and rejects inactive accounts, and the frontend calls it on every page load. A token-version column or
denylist is listed as a future improvement.

### D-23 — Login rate limiting is in-memory, keyed by email + IP
5 failures per 15 minutes → HTTP 429. Simple and dependency-free; resets on restart and is per-instance
(a shared store such as Redis would be needed when running several backend instances).

### D-24 — Generic login errors and constant-time-ish checks
Unknown email, wrong password and inactive account all return "Invalid email or password." For unknown emails a
BCrypt comparison against a dummy hash still runs, so response time does not reveal which accounts exist.
All attempts are written to `audit_logs` (never the password).

### D-25 — Demo users come from a repeatable dev migration
`db/dev/R__dev_seed_users.sql` (dev and test profiles only) inserts fictional employees with
`ON DUPLICATE KEY UPDATE`, so it is idempotent. Production never loads `db/dev`.

### D-26 — Tests can run without Docker
`-Dbankpro.test.local-db=true` + `-Dspring.datasource.*` points the integration tests at an existing empty MySQL
schema instead of Testcontainers. The assistant uses this in its sandbox (MySQL 8.0); the PC uses Docker (8.4).

### D-27 — Post-login redirect is role-checked and internal-only
After sign-in the user returns to the page that required it only if their role may open it and the target is an
internal path; otherwise they land on the dashboard. Signing out does not remember the current page.

### D-28 — npm scripts call `node <tool>` directly
npm's Windows `.cmd` launchers break when the project path contains `&` (as in "Customer & Sales CRM").
The scripts in `frontend/package.json` therefore run `node node_modules/vite/bin/vite.js` etc., which works
from any folder name, in cmd and PowerShell alike.

### D-29 — Demo customers in `R__dev_seed_z1_customers.sql`
Flyway runs repeatable migrations in name order, so "z1" guarantees customers load after the demo users they
reference. Rows match on the unique phone and are never overwritten, so edits made in the app survive re-runs.
Creation dates are relative to the first run (spread over ~6 months) so charts always look current.

### D-30 — API responses carry the caller's permissions
Customer responses include `permissions { canEdit, canArchive }` computed by the same service rules that enforce
them. The UI uses it to show/hide buttons; the backend still rejects any request outside those rules.

### D-31 — CSV export: stricter scope and a row cap
Export uses `DataScope.of` (officers: only their own customers), unlike the list view (officers: whole branch).
Max 5,000 rows per export; every export is audit-logged with row count and scope (no customer data in the log).

### D-32 — Optimistic locking checked against the client's version
The edit form sends the `version` it loaded. If the record changed since, the API answers 409 instead of silently
overwriting a colleague's edit.

### D-33 — Lead stage rules live in one pure class (`LeadStageRules`)
Forward one stage at a time, back one stage to correct a mistake, any open stage → LOST (reason required),
LOST → CONTACTED by managers only, CONVERTED only through the convert action. The UI shows the moves the API
returns in `allowedStatuses`, so frontend and backend never disagree.

### D-34 — Conversion: row lock + idempotency + unique constraint
`POST /leads/{id}/convert` locks the lead (`SELECT … FOR UPDATE`), returns the existing sale if one exists
(HTTP 200, `alreadyConverted: true`), otherwise creates it (201). `UNIQUE(sales.lead_id)` is the last safety net.
Verified by a test that fires six concurrent conversions: exactly one sale is created.

### D-35 — "Sale" means simulated, and loans/cards need a simulated approval
Deposit/account/SME conversions are COMPLETED immediately; loan and card conversions are PENDING_APPROVAL until a
branch manager records APPROVE/DECLINE. Only COMPLETED sales add a product holding and count as sales.

### D-36 — Conversion rate = converted ÷ (converted + lost)
Measured over leads that reached an outcome, so open leads don't drag the rate down; shown as "—" when nothing has
closed yet. Pipeline value = sum of estimated values of open leads, always labelled as an estimate.

### D-37 — Follow-up times are entered and shown in BST
`datetime-local` input is sent with an explicit `+06:00` offset; the backend stores UTC. "Today", "upcoming" and
"overdue" are computed in Asia/Dhaka. Scheduling in the past is rejected (log an interaction instead).

### D-38 — Overdue reminders: scheduled job + dedupe key
Every 5 minutes a job notifies the owner of each overdue follow-up once (`FOLLOW_UP_OVERDUE:{id}:{scheduledAt}`),
backed by `UNIQUE(user_id, dedupe_key)`. Disabled in tests (`app.scheduling.enabled=false`), which call it directly.

### D-39 — Completing a follow-up writes history in the same transaction
The outcome becomes a `customer_interactions` row, and the customer's last-interaction and the lead's
last-contacted dates are updated together.

### D-40 — Metrics are computed live with SQL aggregations
`MetricsRepository` runs `NamedParameterJdbcTemplate` queries (counts, sums, month buckets) instead of loading
entities into Java. No numbers are stored or cached, so the dashboard always matches the records. Nothing is
hard-coded: an empty database shows zeros and "No data" states.

### D-41 — One scope rule for every metric (`MetricScope`)
Relationship officer = own branch + own records; branch manager = own branch; admin = all branches or the
chosen `branchId`. The scope is built from the signed-in user on the server, so changing `branchId` in the URL
cannot widen what a manager or officer sees.

### D-42 — Period = Bangladesh calendar dates
`from`/`to` are inclusive Asia/Dhaka dates converted to UTC bounds; months are bucketed with
`CONVERT_TZ(col,'+00:00','+06:00')`. Default dashboard range: last 6 months.

### D-43 — Metric definitions
- Completed sale date = `COALESCE(decided_at, converted_at)`, so an approved loan/card counts when it is approved.
- Leads converted = sales created in the range, declined applications excluded.
- Conversion rate = converted ÷ (converted + lost); open leads are not counted as failures.
- Deposit sales = deposit-product part of completed sales (volume, not balances).
- Achievement = actual ÷ target × 100 (may exceed 100); shown as "—" when there is no target.
- KPI "Target achievement" = average achievement of the targets in scope that overlap the range.

### D-44 — Targets
Monthly or quarterly, per officer or whole branch, five categories (new customers, deposit sales BDT, credit
card sales, loan leads, converted leads). Only a branch manager creates/edits/deletes, and only for their
branch (`@PreAuthorize` + service check). Count targets must be whole numbers. A duplicate (same owner, period,
category) is rejected with a clear message. Every change is audit-logged. Values are fictional planning
figures, not real bank figures.

### D-45 — One generic report format
Every report returns `{columns, summary, rows, page, chart}`, so a single React page renders all seven
reports and the CSV export uses the same definition (formula-injection safe, audit-logged, row cap).

### D-46 — Chart rules
Recharts with one measure per chart (never two y-axes), a validated colour order (blue first), neutral grey for
"Lost", legends only for 2+ series, hover tooltips, and a "Show data" table on every dashboard chart so the
numbers are readable without the graphic. Single numbers are KPI tiles, not charts. The dashboard and reports
pages are lazy-loaded so the sign-in page does not download the chart library.

### D-47 — Global search reuses the scoped list endpoints
The top-bar search calls `GET /api/customers?q=` and `GET /api/leads?q=` (5 results each) instead of a new
search API. It can therefore never show a record the user could not already open, and there is no second
set of access rules to keep in sync. It is an ARIA combobox: arrow keys, Enter, Escape, `/` or Ctrl+K to focus.

### D-48 — Tables become cards on phones
Below 640 px `DataTable` renders each row as a labelled card with a "Sort by" select, instead of a table
that must be scrolled sideways. The switch uses `matchMedia` (not CSS-only duplication), so there is one copy
of the content in the page for screen readers and tests.

### D-49 — Accessibility baseline: axe-core on every page
Phase 6 checks every page as officer, manager and admin at 1440 px and 390 px with axe-core
(WCAG 2 A/AA + best practice): 96 page views, 0 violations, no horizontal page overflow, no console errors.
Fixes made: every page has one `<h1>` (incl. 403/404), wide tables and the lead board are keyboard-focusable
scroll regions, and the lead board uses real list markup.

### D-50 — Route-level code splitting
Signed-in pages are loaded with `React.lazy` behind one `<Suspense>` in the app shell. The first download
fell from 504 kB to 295 kB (94 kB gzipped), and the chart library loads only on the dashboard and reports.

### D-51 — An access-control matrix test instead of scattered checks
`AccessControlMatrixIntegrationTest` lists every write endpoint and the sensitive reads, and tries each as
another branch, a colleague and the wrong role, plus parameter tampering and missing/forged tokens. New
endpoints should be added to the matrix. Out-of-scope records answer 404 (existence is not revealed);
role violations answer 403.

### D-52 — Browser E2E tests live in their own `e2e/` package
Playwright is kept out of the frontend's dependencies so `npm install` for the app stays small and the
app's unit tests stay fast. The suite runs against a running app (local or deployed via `E2E_BASE_URL`),
one test at a time because the tests share a database.

### D-53 — No customer data in logs
Database constraint errors are logged by constraint name only, and Hibernate's SQL error logger is off,
because MySQL's duplicate-key message contains the duplicated value (a phone number or email). Client
disconnects are logged at debug level. Expected user mistakes (duplicates) are checked before saving so
they are a clean 409, with the database constraint as the guard against races.

