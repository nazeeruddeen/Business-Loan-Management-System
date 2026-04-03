# Business Loan Management System

Business lending platform covering borrower onboarding, KYC document readiness, eligibility evaluation, reviewer routing, disbursement, repayment servicing, dashboard KPIs, and disbursement reporting.

## Project Story

This project is the strongest full-stack lending application in the production suite.

It demonstrates:
- secured Spring Boot APIs with JWT-based access control
- borrower onboarding with address capture
- borrower document intake and KYC completeness checks
- KYC-gated application submission and reviewer assignment
- versioned eligibility rules so policy changes remain auditable
- eligibility evaluation and workflow state tracking
- disbursement, repayment servicing, and installment tracking
- dashboard metrics and disbursement CSV exports
- Docker, Jenkins, and Kubernetes delivery artifacts
- operational recovery notes in `RUNBOOK.md`

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Data JPA / Hibernate
- MySQL
- Spring Security + JWT
- Flyway
- Angular
- Docker
- Jenkins
- Kubernetes

## Local bootstrap access

The backend only seeds bootstrap users when all of the following are true:

- the `local` Spring profile is active
- `APP_SECURITY_BOOTSTRAP_USERS_ENABLED=true`
- bootstrap passwords are supplied through environment variables

Usernames default to `admin`, `officer`, `reviewer`, and `borrower`, but passwords are no longer committed in the repository.

## Ports

- Backend API: `http://localhost:8010`
- Swagger UI: `http://localhost:8010/swagger-ui.html`
- Frontend dev server: `http://localhost:4300`

## Run Locally

Backend:

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Provide datasource credentials, JWT secret, and any local bootstrap passwords through environment variables before using direct backend startup.

Frontend:

```bash
cd frontend
npm install
npm start
```

To run the full local stack with bootstrap users, create a local `.env` from `.env.example` and then start:

```bash
docker compose up -d --build
```

## Main Workflow

1. Sign in from the Angular console using a seeded user.
2. Create a borrower with address details.
3. Upload borrower document metadata and mark required documents as verified.
4. Run eligibility evaluation.
5. Create a draft loan application.
6. Submit the application after KYC is complete. If KYC or eligibility fails, the console now shows a workflow blocker instead of a generic failure.
7. Assign a reviewer.
8. Approve or reject the application.
9. Disburse the approved application.
10. Record repayments and review dashboard/report updates.

## Production deployment posture

- backend pods run as a rolling two-replica deployment in Kubernetes
- application secrets and connection settings are expected to come from an External Secrets store, not committed manifest values
- the in-repo MySQL manifest is for integration environments only; production should use a managed HA database endpoint
