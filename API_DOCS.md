# Business Loan Management System API Docs

Base path: `/api/v1`

## Borrowers

- `POST /borrowers`
- `GET /borrowers/{borrowerId}`
- `GET /borrowers?businessPan=&businessName=`

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

## Loan Accounts

- `GET /loan-accounts/{accountNumber}`
- `GET /loan-accounts/application/{applicationId}`
- `GET /loan-accounts`
- `POST /loan-accounts/{accountId}/repayments`
- `GET /loan-accounts/dashboard`

## Reports

- `GET /reports/disbursements`
- `GET /reports/disbursements/export`

