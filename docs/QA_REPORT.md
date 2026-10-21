# BankPro — Quality Assurance Report (Phase 7)

Date: 2026-10-20 · Scope: everything built in Phases 1–6 · Data: synthetic demo data only.

## 1. Summary

| Check | Result |
|---|---|
| Backend tests (`mvn test`) — JUnit 5, MockMvc, real MySQL | **158 / 158 passed** |
| Frontend tests (`npm test`) — Vitest + React Testing Library | **70 / 70 passed** (23 files), also 15 / 15 runs under heavy CPU load |
| Frontend lint (`npm run lint`, oxlint) | **0 warnings, 0 errors** |
| Frontend production build (`npm run build`) | **OK** — first download 295 kB (94 kB gzipped), no chunk warnings |
| Browser end-to-end tests (`e2e/`, Playwright, Chromium) | **20 / 20 passed**, 4 consecutive full runs, no flaky tests |
| Accessibility + layout audit (axe-core 4.10, WCAG 2 A/AA): 16 pages × 3 roles × 2 widths | **96 / 96 page views clean** — no violations, no sideways scrolling, no console errors |
| Backend log during a full E2E run | **Clean** — only the expected dev-only `JWT_SECRET` notice; no customer data |

All results above were produced in the assistant's sandbox (JDK 21, MySQL 8.0, Chromium 141). The PC results
(JDK 25, MySQL 8.4) are recorded in `PROJECT_STATUS.md`.

## 2. Coverage of the brief's testing requirements (§18)

| Requirement | Where it is tested |
|---|---|
| Authentication success and failure | `AuthIntegrationTest` (login, generic failure, inactive user, bad/forged tokens, rate limit); E2E `forms.spec` |
| Password hashing | `PasswordHashingTest` (salted BCrypt, every seeded hash matches the documented password) |
| Role-based authorization | `AuthorizationIntegrationTest`, **`AccessControlMatrixIntegrationTest`** (new), E2E `access.spec` |
| Branch-level data isolation | `CustomerIntegrationTest.branchIsolation`, `LeadIntegrationTest.leadScope`, `ReportIntegrationTest.reportScope`, **`AccessControlMatrixIntegrationTest`**, E2E `access.spec` |
| Customer validation | `CustomerIntegrationTest.duplicatesAndValidation`, `ContactNormalizerTest`, `CustomerFormPage.test`, E2E `forms.spec` |
| Duplicate lead-conversion prevention | `LeadIntegrationTest.concurrentConversion` (6 threads, one sale), `BankProApplicationTests.databaseRejectsSecondSaleForSameLead` |
| Lead-stage validation | `LeadStageRulesTest` (17 cases), `LeadIntegrationTest.stageRules` |
| Sales target calculation | `DashboardAndTargetsIntegrationTest.targetCalculation`, `achievementGuard` |
| Dashboard aggregation | `DashboardAndTargetsIntegrationTest.kpisMatchDatabase` (KPIs = independent SQL), `chartSeries`; E2E workflow (KPI rises by exactly the sale amount) |
| Follow-up completion | `FollowUpIntegrationTest.scheduleAndComplete`, `FollowUpsPage.test`, E2E `workflow.spec` |
| CSV export safety | `CsvWriterTest`, `CustomerIntegrationTest.csvExport`, `ReportIntegrationTest.productSalesAndCsv`, E2E `exports.spec` (real downloaded file) |
| Frontend: login validation, protected routes, customer form, lead status, search/filter, dashboard states, follow-up completion | `LoginPage`, `ProtectedRoute`, `CustomerFormPage`, `LeadBoard`/`LeadsPage`, `CustomersPage`/`GlobalSearch`/`useSearchText`, `DashboardPage`, `FollowUpsPage` tests |
| Integration: auth + API access, customer CRUD, lead-to-sale, target achievement | MockMvc integration suites above + E2E `workflow.spec` |

## 3. Role restrictions and branch isolation (Phase 7 focus)

**`AccessControlMatrixIntegrationTest`** (52 tests) uses real Motijheel record IDs and tries to reach them as:

- **another branch** (Gulshan manager and officer): read, edit, timeline, log interaction, move/convert a lead,
  create leads/follow-ups for that customer, complete/reschedule a follow-up, decide a sale, change/delete a
  target, read someone's notification, assign a customer or set a target for an officer of another branch;
- **a colleague in the same branch** (Nusrat on Tanvir's records): edit customer, read/move/convert lead,
  complete follow-up, read notification;
- **the wrong role**: officer reassigns/decides/archives/creates targets/views team; admin creates customers,
  moves leads, decides sales, creates targets; manager creates products;
- **parameter tampering**: a foreign `branchId`/`officerId` on customer and lead lists, dashboard, reports,
  CSV export and assignable-staff lists;
- **no token / forged token** on every protected endpoint.

Every attempt is refused (404 for records outside the caller's scope so their existence is not revealed,
403 for actions the role cannot take, 400 when a request names a record the caller cannot use), and tampered
parameters fall back to the caller's own scope. A deliberate mutation (expecting 200) made the suite fail,
confirming the checks are live. The same probes were first run as a black-box script against the running
API: 97/97 refused.

## 4. End-to-end workflow, forms, navigation and exports (`e2e/`)

| Spec | What it proves |
|---|---|
| `workflow.spec` | Officer creates a customer → loan lead → New→Contacted→Qualified→Proposal → converts (pending approval) → manager approves from the queue → lead shows a completed sale → dashboard "Completed sales" rises by exactly ৳750,000 → product-sales report lists it and exports a CSV → the customer appears in the customer CSV. Follow-up scheduled and completed with an outcome. |
| `access.spec` | Officer has no Team area (nav or URL), cannot open a colleague's lead by URL; another branch's customer is not found by URL or search; admin has no create/edit/stage controls; signed-out users return to their page after sign-in. |
| `forms.spec` | Login (required, generic wrong-password message), customer (required, phone format, server-side duplicate), lead (required, negative value), follow-up (past time refused), target (whole numbers, duplicate refused), phone normalisation. |
| `navigation.spec` | Every sidebar page for officer, manager and admin opens with a heading and no errors; filters survive reload and back/forward; unknown URLs show the in-app 404. |
| `exports.spec` | A customer named `=HYPERLINK(…)` with occupation `+SUM(1,2)` exports with a leading `'` so spreadsheets treat it as text; every report's CSV header matches its on-screen table. |

## 5. Defects found and fixed in Phase 7

| # | Defect | Severity | Fix | Regression test |
|---|---|---|---|---|
| 1 | Customer personal data in logs: on a duplicate phone/email race, Hibernate and the error handler logged the raw database message, which contains the duplicated phone number or email. | Medium (privacy) | Error handler logs only the constraint name; Hibernate's SQL error logger is turned off (our handler logs a safe line instead). | `GlobalExceptionHandlerTest`; E2E log check (0 phone numbers in the log) |
| 2 | A duplicate target was detected only by the database unique key, logging an ERROR with stack trace for an ordinary user mistake; the message also showed the raw enum (`LOAN_LEADS`). | Low | `TargetService` checks for the same owner/period/category first (the unique key still guards races); readable message. | `DashboardAndTargetsIntegrationTest` (branch-wide duplicate + message) |
| 3 | Leaving a page while it was loading logged "Unexpected error … Broken pipe" with a stack trace. | Low (log noise) | Client-disconnect exceptions are logged at debug level, no response attempted. | Backend log clean after 4 E2E runs |
| 4 | Customer picker (lead and follow-up forms) could not be operated with arrow keys; options were buttons inside `role="option"`. | Medium (accessibility) | Real ARIA combobox: arrow keys, Enter, Escape, `aria-activedescendant`. | `CustomerPicker.test` |
| 5 | Approval queue buttons were all named just "Approve"/"Decline" for screen readers. | Low (accessibility) | Each button names the customer and product, e.g. "Approve Jahangir Alam's BankPro Personal Loan application". | E2E `workflow.spec` uses the names |
| 6 | On a slow machine, the first letter typed into the Customers/Leads search box could disappear ("Sohel" became "ohel"). Found when `npm test` failed once on the developer PC; reproduced by running the suite under heavy load (2 of 4 runs failed). Cause: the page treated its own, slightly delayed URL update as a change from elsewhere and reset the box. | Medium (usability) | `useSearchText` now ignores URL changes that it made itself. | `useSearchText.test` — 15 / 15 runs pass under the same load |

No defect was found in authorization, branch isolation, calculations or CSV safety.

## 6. Known limitations (unchanged by QA)

- This is a portfolio application with fictional data. It is **not** certified, audited or compliant for real
  banking use and must not be connected to real systems or customer data.
- Loan/card "approvals" are simulated manager decisions; there is no credit assessment.
- Login rate limiting is in memory (per server instance); a multi-instance deployment would need a shared store.
- JWTs are held in memory/session storage by the SPA and expire after 60 minutes; there is no refresh token.
- The backend accepts a request body before checking the caller's role, so an invalid body from a user without
  permission gets a 400 instead of a 403. No data is read or changed either way.
- E2E tests add demo records to the database each run (all named "E2E …").

## 7. How to reproduce

```
# backend (directory: backend)            → expect 158 tests, 0 failures
mvn test
# frontend (directory: frontend)          → expect 70 passed, lint clean, build OK
npm test
npm run lint
npm run build
# end-to-end (directory: e2e; backend + frontend must be running)   → expect 20 passed
npm install
npm run install-browser
npm test
```
