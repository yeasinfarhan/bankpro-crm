# BankPro — Project Status

_Last updated: 2026-10-21_

## Current phase
**Phase 8 (Deployment) — prepared and rehearsed; hosting accounts and go-live pending (developer).**
Guide: `docs/DEPLOYMENT.md`. Next after go-live: Phase 9 (portfolio polish) when asked.

## Completed
- [x] Phase 0 — environment inspected
- [x] Phase 1 — `docs/ARCHITECTURE.md`, `DECISIONS.md`
- [x] Phase 2 — backend + frontend skeleton, Flyway V1 schema (12 tables) + V2 reference data, docker-compose MySQL
- [x] Phase 3 — authentication & authorization
  - Backend: `POST /api/auth/login`, `GET /api/auth/me`, `GET /api/users` (team, scoped), `GET /api/branches` (scoped)
  - JWT (HS256, 60 min) via Spring Security resource server; BCrypt(10) passwords
  - Role checks (`@PreAuthorize`) + `DataScope` branch/ownership scoping in services
  - Login rate limiting (5 failures / 15 min per email+IP → 429), generic login errors, audit log of logins
  - Consistent JSON errors for 400/401/403/404/409/429/500, CORS allow-list, security headers
  - 7 fictional demo users (`db/dev/R__dev_seed_users.sql`), password `BankPro@2026`
  - Frontend: login page (validation, demo accounts), session restore, auto-logout on expiry/401,
    protected routes, role guard, role-filtered sidebar (collapsible + mobile drawer), top bar,
    notifications panel (empty state), user menu, dashboard welcome, team page, settings, 403/404 pages

- [x] Phase 4a — customers & product catalogue
  - Backend: `/api/products` (list/get; ADMIN create/update/deactivate), `/api/customers` (server-side search,
    filters, sort whitelist, pagination; create; update with optimistic locking; archive/restore; CSV export),
    `/api/users/assignable`
  - Rules: branch-scoped reads, officers edit only their own customers, managers archive, ADMIN read-only for
    customers; phone/email normalised before duplicate checks; CSV formula-injection protection; audit log entries
  - 30 fictional demo customers + 23 product holdings (`db/dev/R__dev_seed_z1_customers.sql`)
  - Frontend: Customers list (URL-synced filters, sortable table, pagination, CSV download), customer detail,
    create/edit form, Banking Products catalogue + admin product form; shared Button, FormField, SearchBar,
    SelectFilter, DataTable, Pagination, ConfirmDialog, Toasts

- [x] Phase 4b — leads, conversion, follow-ups, notifications
  - Backend: `/api/leads` (search, board, summary, create, update, PATCH status, PATCH assign, POST convert),
    `/api/sales` (+ manager decision), `/api/follow-ups` (views, counts, create, complete, reschedule),
    `/api/customers/{id}/timeline` + `/interactions`, `/api/notifications`, overdue-reminder job
  - 30 demo leads, 11 simulated sales (completed / pending / declined), 14 follow-ups, interaction history
    (`db/dev/R__dev_seed_z2_leads.sql`, `R__dev_seed_z3_followups.sql`)
  - Frontend: Kanban board (drag & drop + keyboard "Move" menu), table view, pipeline summary, lead detail with
    stage track, convert / lost / assign dialogs, manager approval queue, Follow-ups tabs + month calendar,
    customer leads / follow-ups / timeline / log interaction, live notification bell

- [x] Phase 5 — dashboard, sales targets, reports
  - Backend: `/api/dashboard/*` (summary + 6 chart/table feeds), `/api/targets` (+ ranking; manager CRUD),
    `/api/reports/{type}` + CSV export (7 report types); live SQL aggregations scoped by role (D-40..D-46)
  - Demo targets for the current and previous month + quarterly branch targets (`R__dev_seed_z4_targets.sql`)
  - Frontend: KPI tiles with definitions, period presets, admin branch filter, Recharts charts with
    "Show data" tables, targets progress + ranking + manager create/edit/delete, generic reports page
  - Details: `docs/PHASE5_NOTES.md`

- [x] Phase 6 — professional UI
  - Working global search (customers + leads, role-scoped, keyboard accessible, mobile overlay)
  - Phone layout: 2-column KPI tiles, tables as labelled cards with "Sort by", logo + search in the mobile top bar
  - Accessibility: axe-core clean on all pages for all roles at desktop and phone width (D-49)
  - Consistent filter bars and input heights; Team page uses the shared table; unused placeholder page removed
  - Route-level code splitting: first download 504 kB → 295 kB (D-50)
- [x] Phase 7 — quality assurance (`docs/QA_REPORT.md`)
  - Access-control matrix test: 52 wrong-branch / wrong-owner / wrong-role / tampering checks (D-51)
  - Browser E2E suite in `e2e/` (Playwright, 20 tests): full customer-to-sale workflow, access, forms, navigation, exports (D-52)
  - Fixed: customer data in error logs (D-53), duplicate-target error logging, broken-pipe log noise,
    keyboard support in the customer picker, ambiguous Approve/Decline button names,
    lost first letter in list search boxes on slow machines
- [~] Phase 8 — deployment (`docs/DEPLOYMENT.md`)
  - Done: backend Dockerfile (512 MB-sized JVM), `prod` hardening, `demo` profile, `render.yaml` Blueprint,
    `frontend/vercel.json` (SPA routing, CSP and security headers), full-stack `docker compose --profile app`,
    sleeping-server notice on sign-in, read-only `npm run smoke` for the live site (D-54…D-57)
  - Pending (developer): create Aiven / Render / Vercel accounts, deploy, set CORS, run `npm run smoke`

## Verified results
| Check | Where | Result |
|---|---|---|
| Backend `mvn test` (Phase 2 code) | Developer PC (JDK 25, Docker 29, MySQL 8.4) | Passed (reported by developer) |
| Backend `mvn test` (Phase 3 code) — 26 tests | Assistant sandbox (JDK 21, MySQL 8.0, offline with PC's Maven cache) | 26 / 26 passed |
| App start + real HTTP login / 403 / Swagger | Assistant sandbox | Passed |
| Frontend lint / tests / build | Assistant sandbox | 0 warnings · 24 / 24 passed · build OK |
| Browser end-to-end (login, redirect, reload, sign-out, officer blocked from Team, mobile 390 px) | Assistant sandbox, Chromium | Passed after fixing 2 redirect bugs |
| Backend `mvn test` (Phase 4a) — 59 tests | Assistant sandbox (MySQL 8.0) | 59 / 59 passed |
| Frontend lint / tests / build (Phase 4a) | Assistant sandbox | 0 warnings · 35 / 35 passed · build OK |
| Browser end-to-end (officer create/search/edit/duplicate/export, manager archive/restore/sort, admin product, mobile) | Assistant sandbox, Chromium | Passed |
| Backend `mvn test` (Phase 4b) — 92 tests | Assistant sandbox (MySQL 8.0) | 92 / 92 passed (incl. 6-thread concurrent conversion) |
| Frontend lint / tests / build (Phase 4b) | Assistant sandbox | 0 warnings · 49 / 49 passed · build OK |
| Browser end-to-end (board menu + drag, illegal drag blocked, card conversion → approval, follow-up complete/schedule, calendar, timeline, reassignment notification, admin read-only, mobile) | Assistant sandbox, Chromium | Passed |
| Phase 4b on the PC — 92 tests + app start | Developer PC (JDK 25, MySQL 8.4) | 92 / 92 passed, app started (developer log, 2026-10-20 20:08) |
| Backend `mvn test` (Phase 5) — 105 tests | Assistant sandbox (MySQL 8.0) | 105 / 105 passed |
| Frontend lint / tests / build (Phase 5) | Assistant sandbox | 0 warnings · 61 / 61 passed · build OK |
| Browser end-to-end (officer / manager / admin dashboards, branch filter vs API, target create / duplicate / edit / delete, all 7 reports, CSV export, mobile 390 px) | Assistant sandbox, Chromium | Passed, no console errors |
| Phase 5 backend on the PC — 105 tests + app start | Developer PC (JDK 25, MySQL 8.4) | 105 / 105 passed, target seed applied, app started (developer log, 2026-10-20 20:42) |
| Phase 5 frontend on the PC (`npm test`, pages in browser) | Developer PC | Confirmed by developer ("phase 5 done") |
| Frontend lint / tests / build (Phase 6) | Assistant sandbox | 0 warnings · 68 / 68 passed · build OK, no chunk-size warning |
| Accessibility + layout audit (Phase 6): 16 pages × 3 roles × 2 widths | Assistant sandbox, Chromium + axe-core 4.10 | 0 violations · no horizontal overflow · no console errors |
| Browser end-to-end (global search scope per role, keyboard, see-all, mobile overlay, mobile card sort) | Assistant sandbox, Chromium | Passed |
| Phase 6 frontend on the PC (`npm test`) | Developer PC | 68 / 68 passed (reported by developer, 2026-10-20) |
| Backend `mvn test` (Phase 7) — 158 tests | Assistant sandbox (MySQL 8.0) | 158 / 158 passed |
| Frontend lint / tests / build (Phase 7) | Assistant sandbox | 0 warnings · 70 / 70 passed · build OK |
| Browser E2E suite (`e2e/`, 20 tests) | Assistant sandbox, Chromium | 20 / 20 passed, 4 consecutive runs |
| Accessibility audit re-run (96 page views) | Assistant sandbox, axe-core 4.10 | 0 violations |
| Phase 7 backend on the PC — `mvn test` | Developer PC (JDK 25, MySQL 8.4) | 158 / 158 passed (reported by developer, 2026-10-20) |
| Phase 7 frontend on the PC — `npm test` | Developer PC | 70 / 70 passed after the search-box fix (reported by developer, 2026-10-21) |
| Backend `mvn test` (Phase 8) — 159 tests | Assistant sandbox (MySQL 8.0) | 159 / 159 passed |
| Frontend lint / tests / build (Phase 8) | Assistant sandbox | 0 warnings · 72 / 72 passed · build OK |
| Production rehearsal: `prod,demo` profiles, TLS to MySQL with `sql_require_primary_key=ON` (as Aiven), 256 MB heap / 512 MB-sized JVM flags, URL-safe JWT secret, separate frontend origin with vercel.json CSP headers | Assistant sandbox | Fresh DB migrated + seeded; all 20 E2E + 6 smoke tests passed; 0 CSP violations; RSS ~367 MB; log clean |
| Docker images + production stack (`docker compose --profile app`: prod,demo profiles, 512 MB limit, nginx) | Developer PC (Docker Desktop) | Both images built; `npm run smoke` 6 / 6 passed (2026-10-21) |
| Live deployment (Aiven + Render + Vercel) | — | **Pending (developer)** |
| Backend `mvn test` + app run (Phase 3 + 4a) on the PC | Developer PC (JDK 25, Docker MySQL 8.4) | Passed — tests, login and Customers page confirmed by developer (2026-10-19 13:45) |
| `mvn spring-boot:run` (Phase 3) on the PC | Developer PC, MySQL 8.4.11 (Docker) | Started on port 8080 — Flyway + Hibernate validation OK (2026-10-19 10:34) |
| Login in the browser on the PC | Developer PC | Working (confirmed 2026-10-19) |

### Backend tests (26)
AuthIntegrationTest (8): login success incl. trimmed/mixed-case email, generic failure, inactive user, validation,
rate limit 429, /me, missing/tampered/expired/foreign-issuer tokens, public endpoint + security headers ·
AuthorizationIntegrationTest (5): admin sees all users, manager only own branch (both branches), officer 403,
branch visibility, CORS allow/deny · BankProApplicationTests (3) · SystemControllerTest (1) ·
PasswordHashingTest (2) · DataScopeTest (5) · LoginRateLimiterTest (2)

## Environment (developer PC, Windows 11)
Java 25.0.1 · Maven 3.9.11 · Node 22.17.0 · Git 2.48.1 · Docker 29.8.2 · MySQL via Docker only.
npm needs `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned` once.

## Commands
```powershell
# repo root
docker compose up -d
# backend
cd backend; mvn test; mvn spring-boot:run          # http://localhost:8080 (PORT in .env to change)
# frontend
cd frontend; npm install; npm run dev              # http://localhost:5173
npm test; npm run lint; npm run build
# end-to-end (backend + frontend running)
cd e2e; npm install; npm run install-browser; npm test
```

## Pending
_Git: first commit `a040c45` (Phases 0–5) pushed to GitHub `origin/main` on 2026-10-20._

- [ ] Delete `C:\Users\Farhan\.m2\repository\_bankpro_tmp` and the project's `_to_delete` folder (assistant's temporary copies)
- [ ] Phases 6–9 — UI polish, QA, deployment, portfolio

## Known bugs / issues
- Port 8080 was occupied on the PC (environment). Use `PORT=8081` in `.env` + `VITE_DEV_PROXY_TARGET` in
  `frontend/.env.local` if it can't be freed.

## Next step
Follow `docs/DEPLOYMENT.md` (optional step 0: `docker compose --profile app up -d --build`), then send the live addresses for verification.
