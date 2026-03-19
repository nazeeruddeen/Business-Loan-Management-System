# Business Loan Management System

Business lending backend and dashboard built with Spring Boot, MySQL, JWT security, and Angular.

## What this project covers

- Borrower onboarding with business identity and address capture
- Loan product management
- Eligibility evaluation and rule management
- Loan application workflow from draft to disbursement
- Repayment schedule generation and repayment recording
- Disbursement reporting and CSV export
- Role-based security, validation, and structured exception handling

## Stack

- Java 17
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL
- Flyway
- Angular 17
- Docker, Jenkins, Kubernetes

## Structure

- `backend/` - Spring Boot service
- `frontend/` - Angular dashboard shell
- `k8s/` - deployment manifests

## Ports

- Backend: `8010`
- Frontend: `4300`

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

## Interview story

Use this project as the core backend delivery story:

1. Borrower and product setup
2. Eligibility evaluation
3. Application review and approval
4. Disbursement and servicing
5. Reporting and exports
