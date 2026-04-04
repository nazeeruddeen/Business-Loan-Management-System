# Business Loan Management System API Docs

Base path: `/api/v1`

Authentication base path: `/auth`

## Authentication

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`

## User Management

- `GET /auth/users`
- `POST /auth/users`
- `PATCH /auth/users/{id}/role`
- `PATCH /auth/users/{id}/status`
- `PATCH /auth/users/{id}/password`

## Borrowers

- `POST /borrowers`
- `PATCH /borrowers/{borrowerId}`
- `GET /borrowers/{borrowerId}`
- `GET /borrowers?businessPan=&businessName=`

Borrower search returns a paginated response shape with `content`, `number`, `size`, `totalElements`, and `totalPages`.

## Borrower KYC Documents

- `POST /borrowers/{borrowerId}/documents`
- `GET /borrowers/{borrowerId}/documents`
- `PATCH /borrowers/{borrowerId}/documents/{documentId}/status`

Required KYC documents are enforced at workflow time. By default:

- `PAN_CARD`
- `BUSINESS_REGISTRATION`
- `BANK_STATEMENT`
- `GST_CERTIFICATE` when GSTIN is present

Workflow note:

- the operator console surfaces missing KYC documents explicitly before submit or reviewer assignment is attempted

## Loan Products

- `POST /loan-products`
- `GET /loan-products/{productId}`
- `GET /loan-products?name=&active=&amount=&maxTenureMonths=`

## Eligibility

- `POST /eligibility/evaluate`
- `POST /eligibility-rules`
- `GET /eligibility-rules`

Eligibility rules are versioned in the database so policy changes remain auditable and visible in the operator console.

## Loan Applications

- `POST /loan-applications`
- `POST /loan-applications/{applicationId}/submit`
- `POST /loan-applications/{applicationId}/assign-reviewer`
- `POST /loan-applications/{applicationId}/decision`
- `POST /loan-applications/{applicationId}/disburse`
- `GET /loan-applications/{applicationId}`
- `GET /loan-applications?status=`

Loan application search returns a paginated response shape with `content`, `number`, `size`, `totalElements`, and `totalPages`.

Workflow notes:

- submission fails if eligibility is not passed
- submission fails if borrower KYC is incomplete
- reviewer assignment also fails if borrower KYC is incomplete

Failures are returned as structured business-rule errors (`422 Unprocessable Entity`) or conflict errors (`409 Conflict`) instead of generic server failures.

Operational note:

- the operator console and API responses share the same workflow blocker language
- correlation IDs and logs should be used together when investigating retry, KYC, or approval failures
- the production runbook in `RUNBOOK.md` captures the recovery path for blocked submissions and workflow conflicts
- production Kubernetes deployments expect datasource credentials, JWT secrets, and connection settings to arrive through External Secrets rather than committed manifest values

## Loan Accounts

- `GET /loan-accounts/{accountNumber}`
- `GET /loan-accounts/application/{applicationId}`
- `GET /loan-accounts`
- `POST /loan-accounts/{accountId}/repayments`
- `GET /loan-accounts/dashboard`

Loan account search returns a paginated response shape with `content`, `number`, `size`, `totalElements`, and `totalPages`.

Repayment servicing notes:

- repayments apply interest first, then scheduled principal
- excess amount beyond currently due installments is applied as principal curtailment when outstanding principal remains
- principal prepayment triggers installment recast instead of being rejected as an overpayment

## Reports

- `GET /reports/disbursements`
- `GET /reports/disbursements/export`
