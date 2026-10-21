# BankPro — Customer & Sales CRM

> Smarter Customer Relationships. Better Banking Sales.

A customer relationship and branch-sales CRM for a **fictional** bank, built as a portfolio project with
Spring Boot, React and MySQL. All data is synthetic. This is not a real banking system and has not been
security- or regulator-reviewed.

**Status:** in development — see [PROJECT_STATUS.md](PROJECT_STATUS.md).
Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · Decisions: [DECISIONS.md](DECISIONS.md)

## Quick start (development)

Prerequisites: JDK 21+ (21 recommended), Maven 3.9+, Node.js 22.12+, Docker Desktop.

```powershell
Copy-Item .env.example .env          # then change the passwords in .env
docker compose up -d                 # MySQL 8.4 on localhost:3306

cd backend
mvn spring-boot:run                  # API on http://localhost:8080

cd ../frontend
npm install
npm run dev                          # UI on http://localhost:5173
```

## Demo accounts (development data, fictional)

All demo accounts use the password **`BankPro@2026`**.

| Role | Email | Branch |
|---|---|---|
| Administrator | admin@bankpro.demo | All branches |
| Branch Manager | manager.motijheel@bankpro.demo | Motijheel |
| Relationship Officer | nusrat.jahan@bankpro.demo | Motijheel |
| Relationship Officer (Sales Officer) | tanvir.ahmed@bankpro.demo | Motijheel |
| Branch Manager | manager.gulshan@bankpro.demo | Gulshan |
| Relationship Officer | imran.hossain@bankpro.demo | Gulshan |
| *(inactive — login is refused)* | former.officer@bankpro.demo | Motijheel |

These accounts exist when the backend runs with the `dev` profile (the default locally) or the `demo` profile (the hosted demo).

## Tests

```powershell
cd backend;  mvn test        # Docker Desktop must be running (Testcontainers MySQL 8.4)
cd frontend; npm test; npm run lint; npm run build
cd e2e;      npm install; npx playwright install chromium; npm test   # app must be running
```

QA results: [docs/QA_REPORT.md](docs/QA_REPORT.md).

## Deployment

Free hosting: Aiven (MySQL) + Render (Spring Boot in Docker) + Vercel (React). Step-by-step guide:
[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md). To run the production setup locally:
`docker compose --profile app up -d --build` → http://localhost:8081.

API documentation (dev): http://localhost:8080/swagger-ui.html — sign in via `POST /api/auth/login`,
then click **Authorize** and paste the `accessToken`.

The full README (features, screenshots, deployment) is completed in Phase 9.
