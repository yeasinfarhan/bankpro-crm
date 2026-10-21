# BankPro — 5-Minute Demo Script

Use this for an interview, a screen-share or a recorded walkthrough. Start the app first
(`docker compose --profile app up -d` → http://localhost:8081, or the dev setup on http://localhost:5173).
All accounts use the password `BankPro@2026`. Times are a guide.

---

## 0:00 — The problem (30 seconds, no clicking)

> "Relationship officers in a branch manage customers, product enquiries and follow-up calls across
> spreadsheets and notebooks, and managers only see results at month end. BankPro is a CRM that puts the
> daily sales workflow — customers, leads, follow-ups, targets — in one place, with each person seeing exactly
> the data their role allows. Everything here is fictional demo data."

## 0:30 — Officer's day (1.5 minutes) · `nusrat.jahan@bankpro.demo`

1. **Sign-in page** — point out the demo-account panel and that the page says "fictional data".
2. **Dashboard** — "These eight figures are calculated live from the database, and each card says exactly how."
   Point at *Target achievement* and *Overdue follow-ups*.
3. **Follow-ups → Today/Overdue** — complete one: click **Complete**, type an outcome
   ("Customer will visit Sunday with documents"). "The outcome is saved to the customer's history automatically."
4. **Leads & Opportunities (board)** — "This is my pipeline." Move a *New* lead to *Contacted* with the
   **Move** menu (or drag it). "The system only offers moves that are allowed — you can't jump from New to Converted."
5. Open a lead in *Proposal* → **Convert to sale** → enter an amount.
   - For a **deposit** product: "It's completed immediately and counts towards my target."
   - For a **loan or card**: "It goes to my branch manager for a simulated decision — the app never assesses credit."

## 2:00 — Manager's view (1.5 minutes) · `manager.motijheel@bankpro.demo`

1. **Leads** — the **Applications awaiting your simulated decision** panel → **Approve**.
   "Approval is a manager-only action, checked on the server, and it's written to the audit log."
2. **Dashboard** — "Completed sales went up by exactly that amount." Show **Officer performance** at the bottom.
3. **Sales Targets** — progress bars and the officer ranking. Click **New target**: "Count targets must be
   whole numbers, and you can't create the same target twice."
4. **Performance Reports → Product sales** — change the period, then **Export CSV**.
   "Exports are protected against spreadsheet formula injection."

## 3:30 — Security in one minute

1. Sign out, sign in as the **Gulshan** manager (`manager.gulshan@bankpro.demo`).
2. Paste a Motijheel customer's URL (e.g. one you opened earlier) → **not found**.
   "Branches are isolated on the server. Changing an ID in the URL doesn't reveal anything — the API answers
   404, so you can't even tell the record exists."
3. Type the same customer's name in the top search → no results.
4. Optional: as the officer, try `/team` → "You don't have access".

## 4:30 — How it's built (30 seconds)

> "Spring Boot and MySQL on the back end, React on the front end. Access rules are enforced in Spring
> Security and in every database query, not just by hiding buttons. There are about 250 automated tests,
> including a matrix that tries every action as the wrong branch and the wrong role, and a browser test of this
> whole journey. It runs with one Docker command and is ready to deploy."

---

## Tips

- Practise once with the timer. If something goes wrong, the **Show data** button on any chart and the
  QA report are good fallbacks.
- On a phone-sized window, show that tables turn into cards — interviewers notice responsive design.
- Know your numbers: 159 backend tests, 72 frontend tests, 20 end-to-end tests, 0 accessibility violations.
