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
- `GET /borrowers/{borrowerId}`
- `GET /borrowers?businessPan=&businessName=`

## Borrower KYC Documents

- `POST /borrowers/{borrowerId}/documents`
- `GET /borrowers/{borrowerId}/documents`
- `PATCH /borrowers/{borrowerId}/documents/{documentId}/status`

Required KYC documents are enforced at workflow time. By default:

- `PAN_CARD`
- `BUSINESS_REGISTRATION`
- `BANK_STATEMENT`
- `GST_CERTIFICATE` when GSTIN is present

## Loan Products

- `POST /loan-products`
- `GET /loan-products/{productId}`
- `GET /loan-products?name=&active=&amount=&maxTenureMonths=`

## Eligibility

- `POST /eligibility/evaluate`
- `POST /eligibility-rules`
- `GET /eligibility-rules`

## Loan Applications

- `POST /loan-applications`
- `POST /loan-applications/{applicationId}/submit`
- `POST /loan-applications/{applicationId}/assign-reviewer`
- `POST /loan-applications/{applicationId}/decision`
- `POST /loan-applications/{applicationId}/disburse`
- `GET /loan-applications/{applicationId}`
- `GET /loan-applications?status=`

Workflow notes:

- submission fails if eligibility is not passed
- submission fails if borrower KYC is incomplete
- reviewer assignment also fails if borrower KYC is incomplete

## Loan Accounts

- `GET /loan-accounts/{accountNumber}`
- `GET /loan-accounts/application/{applicationId}`
- `GET /loan-accounts`
- `POST /loan-accounts/{accountId}/repayments`
- `GET /loan-accounts/dashboard`

## Reports

- `GET /reports/disbursements`
- `GET /reports/disbursements/export`
