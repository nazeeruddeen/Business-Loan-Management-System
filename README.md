# Business Loan Management System

Business lending platform covering borrower onboarding, KYC document readiness, eligibility evaluation, reviewer routing, disbursement, repayment servicing, dashboard KPIs, and disbursement reporting.

## Project Story

This project is the strongest full-stack lending application in the portfolio.

It demonstrates:
- secured Spring Boot APIs with JWT-based access control
- borrower onboarding with address capture
- borrower document intake and KYC completeness checks
- KYC-gated application submission and reviewer assignment
- eligibility evaluation and workflow state tracking
- disbursement, repayment servicing, and installment tracking
- dashboard metrics and disbursement CSV exports
- Docker, Jenkins, and Kubernetes delivery artifacts

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

## Default Users

The backend seeds the following users on startup:

- `admin / Admin@123`
- `officer / Officer@123`
- `reviewer / Reviewer@123`
- `borrower / Borrower@123`

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

Frontend:

```bash
cd frontend
npm install
npm start
```

## Main Workflow

1. Sign in from the Angular console using a seeded user.
2. Create a borrower with address details.
3. Upload borrower document metadata and mark required documents as verified.
4. Run eligibility evaluation.
5. Create a draft loan application.
6. Submit the application after KYC is complete.
7. Assign a reviewer.
8. Approve or reject the application.
9. Disburse the approved application.
10. Record repayments and review dashboard/report updates.
