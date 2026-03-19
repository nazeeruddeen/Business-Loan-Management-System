# Business Loan Management System

Business lending platform built with Spring Boot, MySQL, JWT security, and an Angular dashboard shell. This is the strongest end-to-end backend story in the repo set.

## Highlights

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

## Project Layout

- `backend/` - Spring Boot API service
- `frontend/` - Angular dashboard shell
- `k8s/` - deployment manifests

## Ports

- Backend: `8010`
- Frontend dev-server: `4300`

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

## Interview Talking Points

1. Borrower and product setup
2. Eligibility evaluation
3. Application review and approval
4. Disbursement and servicing
5. Reporting and exports
