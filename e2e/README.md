# BankPro end-to-end tests

Browser tests (Playwright) that drive the real app: sign-in, the full customer → lead → sale → approval
workflow, role and branch restrictions, form validation, navigation and CSV exports.

They need the app running first (backend on port 8080 with the dev profile, frontend on port 5173).
The tests add a few clearly labelled "E2E …" demo records each run; they never delete seed data.

```
cd e2e
npm install
npx playwright install chromium      (first time only — downloads the test browser)
npm test
```

`npm run report` opens an HTML report; failed tests keep a screenshot and a trace in `test-results/`.
Set `E2E_BASE_URL` (frontend) and `E2E_API_URL` (backend, default http://localhost:8080) to test other addresses.
