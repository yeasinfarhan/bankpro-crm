# Phase 5 — Dashboard, Sales Targets, Reports

Status: built and verified on 2026-10-20 (cloud): backend 105/105 tests, frontend 61/61 tests, lint clean,
production build OK, browser end-to-end run as officer, branch manager and admin with no console errors.
Merge the decisions below into DECISIONS.md (as D-40 onward) and the endpoints into ARCHITECTURE.md.

## Decisions

**D-40 — Metrics are computed live with SQL aggregations.**
`MetricsRepository` runs `NamedParameterJdbcTemplate` queries (counts, sums, month buckets) instead of loading
entities into Java. No numbers are stored or cached, so the dashboard always matches the records. Nothing is
hard-coded: an empty database shows zeros and "No data" states.

**D-41 — One scope rule for every metric (`MetricScope`).**
Relationship officer = own branch + own records; branch manager = own branch; admin = all branches or the
chosen `branchId`. The scope is built from the signed-in user on the server, so changing `branchId` in the URL
cannot widen what a manager or officer sees.

**D-42 — Period = Bangladesh calendar dates.**
`from`/`to` are inclusive Asia/Dhaka dates converted to UTC bounds; months are bucketed with
`CONVERT_TZ(col,'+00:00','+06:00')`. Default dashboard range: last 6 months.

**D-43 — Metric definitions.**
- Completed sale date = `COALESCE(decided_at, converted_at)`, so an approved loan/card counts when it is approved.
- Leads converted = sales created in the range, declined applications excluded.
- Conversion rate = converted ÷ (converted + lost); open leads are not counted as failures.
- Deposit sales = deposit-product part of completed sales (volume, not balances).
- Achievement = actual ÷ target × 100 (may exceed 100); shown as "—" when there is no target.
- KPI "Target achievement" = average achievement of the targets in scope that overlap the range.

**D-44 — Targets.**
Monthly or quarterly, per officer or whole branch, five categories (new customers, deposit sales BDT, credit
card sales, loan leads, converted leads). Only a branch manager creates/edits/deletes, and only for their
branch (`@PreAuthorize` + service check). Count targets must be whole numbers. A duplicate (same owner, period,
category) is rejected with a clear message. Every change is audit-logged. Values are fictional planning
figures, not real bank figures.

**D-45 — One generic report format.**
Every report returns `{columns, summary, rows, page, chart}`, so a single React page renders all seven
reports and the CSV export uses the same definition (formula-injection safe, audit-logged, row cap).

**D-46 — Chart rules.**
Recharts with one measure per chart (never two y-axes), a validated colour order (blue first), neutral grey for
"Lost", legends only for 2+ series, hover tooltips, and a "Show data" table on every dashboard chart so the
numbers are readable without the graphic. Single numbers are KPI tiles, not charts. The dashboard and reports
pages are lazy-loaded so the sign-in page does not download the chart library.

## New endpoints (all require sign-in)

| Method | Path | Who |
|---|---|---|
| GET | `/api/dashboard/summary`, `/sales-trend`, `/customer-acquisition`, `/lead-funnel`, `/product-mix`, `/targets` | all roles, scoped |
| GET | `/api/dashboard/officer-performance` | manager, admin |
| GET | `/api/targets?from&to&branchId&userId` | all roles, scoped |
| GET | `/api/targets/ranking` | manager, admin |
| POST / PUT `{id}` / DELETE `{id}` | `/api/targets` | branch manager (own branch) |
| GET | `/api/reports/{type}` and `/api/reports/{type}/export` | all roles, scoped |

Report types: customer-acquisition, lead-pipeline, conversion, product-sales, officer-performance,
target-achievement, follow-up-completion. Common params: `from`, `to`, `branchId` (admin), `officerId`, `page`, `size`.

## Frontend
- Dashboard: 8 KPI tiles with written definitions, period presets (this month, last month, this quarter, last 6 months, this year, custom), branch filter for admin, 4 charts, targets progress, officer-performance table (manager/admin).
- Sales Targets: month picker, branch and officer groups with progress meters, officer ranking, create/edit/delete for branch managers.
- Performance Reports: report selector, filters kept in the URL, summary figures, chart, paginated table, CSV export.

## Known limitations
- Reports chart only the rows on the current page (20 per page).
- Seed data dates are relative to when the database was first seeded, so month totals differ between machines.
