# BankPro — Interview Guide

How to talk about this project in interviews for **Relationship Officer / branch sales** roles and for
**software** roles. Use your own words; the points below are what to cover, not a script to memorise.

---

## 1. The 30-second pitch

> "BankPro is a customer and sales CRM for a bank branch, built as a portfolio project with fictional data.
> Relationship officers manage their customers, track product leads from first contact to a sale, and never miss
> a follow-up; branch managers approve applications, set targets and see live branch performance. I built the
> whole thing — Spring Boot and MySQL back end, React front end — with branch-level data security and about
> 250 automated tests."

Then stop and let them ask. Offer the 5-minute demo ([DEMO_SCRIPT.md](DEMO_SCRIPT.md)) if there is a screen.

## 2. The business problem (for banking interviewers)

- **A relationship officer's job** is to grow and keep a customer portfolio: open accounts, mobilise deposits
  (FDR, DPS, savings), cross-sell cards and loans, and keep in touch so customers don't drift to competitors.
- **What goes wrong without a system:** enquiries are forgotten, follow-ups slip, two officers call the same
  customer, and the manager learns the month's numbers when it is too late to act.
- **What BankPro changes:**
  - every enquiry becomes a **lead** with a stage, an estimated value and an owner;
  - **follow-ups** have a date and an overdue list, and the outcome goes into the customer's history;
  - **targets** (deposit volume, new customers, card sales, loan leads, conversions) show progress daily;
  - the **manager** sees the pipeline, approves loan/card applications and compares officers fairly.

### Banking ideas the project shows you understand

| Idea | Where it appears |
|---|---|
| Sales pipeline and conversion rate | Lead stages; conversion = converted ÷ (converted + lost), so open leads don't count as failures |
| Pipeline value is not a sale | "Estimated opportunity value" is kept separate from "completed sales" everywhere |
| Deposit sales volume vs deposit balance | The dashboard reports sales volume and says plainly it is not a balance |
| Maker–checker | Officers convert; loans and cards wait for the manager's decision (simulated — no credit assessment) |
| Customer data confidentiality | Branch isolation, no personal data in logs, audit trail of who did what |
| Segments | Mass, emerging affluent, affluent and SME customers, used in acquisition reporting |

Be clear that all products, rates and approvals are fictional, and that real credit decisions follow the bank's
policy and regulation, not an app.

## 3. Technical decisions — what and why

Explain each in one or two sentences; the reasons matter more than the names.

1. **Security on the server, not the screen.** Hiding a button stops nobody. Every request is checked by role
   (Spring Security) and every database query is limited to what that user may see (`DataScope`: all, branch or
   own). Records outside your scope return 404, so IDs can't be guessed. *(D-08, D-51)*
2. **No double counting.** One lead can produce at most one sale — guaranteed by a database unique key plus a
   row lock, and proven by a test that converts the same lead from 6 threads at once. *(D-11, D-34)*
3. **Numbers you can trust.** Dashboard figures are SQL calculations over the live data, each with a written
   definition, and the tests compare them with independent SQL. Nothing is hard-coded. *(D-36, D-40–D-43)*
4. **Time zones.** Everything is stored in UTC; "this month" means the Bangladesh calendar month. *(D-12, D-42)*
5. **Database changes are versioned** with Flyway migrations, so every environment has the same schema. *(D-03)*
6. **Testing at three levels:** fast unit tests, integration tests against a real MySQL in Docker, and browser
   tests of real user journeys, plus an automated accessibility audit. Testing on a slower PC found a real bug
   (a lost first letter in the search box) — a good story about why tests on different machines matter. *(QA report #6)*
7. **Privacy in logs:** database error messages contain the duplicated value (a phone number), so the app logs
   only the constraint name. *(D-53)*
8. **Deployment-ready:** Docker images sized for a 512 MB server, production settings (Swagger off, only the
   health check exposed, encrypted DB connection), and a guide for free hosting. *(D-54–D-57)*

## 4. Likely questions and good answers

**"Why did you build a CRM?"** — Because it's the tool a relationship officer uses every day; building it made
me think about the job itself: what to track, what a manager needs to see, and what must stay confidential.

**"How do you stop an officer seeing another branch's customers?"** — The branch and owner come from the
signed-in user's token, never from the request, and every query is filtered by them. I have a test matrix that
tries every action with another branch's IDs, a colleague's records and the wrong role — all are refused.

**"What happens if two people convert the same lead at the same time?"** — The first takes a lock on the lead
and creates the sale; the database also has a unique key on the sale's lead, so a second sale is impossible.
The second request gets the existing result instead of an error.

**"How is target achievement calculated?"** — Actual ÷ target × 100 for the target's own period; actuals come
from completed sales, new customers or conversions in that period. If there is no target, it shows "—"
instead of dividing by zero.

**"What would you add next?"** — User management screens and an audit-log viewer for admins, password reset,
reminders by SMS/email, and a rule-based "next best product" suggestion from the customer's segment and holdings.

**"What was the hardest part?"** — Pick one you can explain honestly, for example the lead stage rules and
idempotent conversion, or keeping every dashboard number consistent with its definition.

**"Is it production-ready for a real bank?"** — No, and I say so in the README: it uses fictional data, has
not had a security or regulatory review, and approvals are simulated. It is built the way a real system should
start — secure by default, tested and documented.

## 5. Numbers to remember

- 3 roles · 7 demo users · 3 branches (2 with demo staff) · 12 database tables · 57 recorded design decisions
- 159 backend + 72 frontend + 20 end-to-end tests; 0 accessibility violations across 96 page checks
- 8 KPIs · 7 reports · 5 target categories · 6 lead stages

## 6. Where to point people

- README (overview, screenshots, one-command Docker run)
- `docs/DEMO_SCRIPT.md` — 5-minute walkthrough
- `docs/QA_REPORT.md` — testing, defects found and fixed
- `DECISIONS.md` — why things are built the way they are
