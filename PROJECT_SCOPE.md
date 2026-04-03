# Business Loan Management System

Product identity:

- flagship enterprise lending application in the production suite

Implemented scope:

- borrower onboarding with address capture
- borrower KYC/document metadata workflow
- KYC completeness visibility and submit/reviewer guards
- versioned eligibility rules
- business loan product management
- eligibility evaluation and application workflow
- reviewer routing, approval, rejection, and disbursement handling
- repayment servicing and installment tracking
- dashboard KPIs and disbursement reporting
- JWT-secured Angular operator console
- explicit workflow blocker messaging for KYC and business-rule failures
- Docker, Jenkins, and Kubernetes delivery assets
- production runbook and recovery notes in `RUNBOOK.md`
- production secret-manager-backed configuration and local-profile-gated bootstrap users

Position in production suite:

- this is the strongest full-stack lending system in the production suite
- it demonstrates the cleanest end-to-end Spring Boot + Angular + delivery story
- it is the best project to discuss layered architecture, workflow control, KYC gating, versioned policy, and repayment servicing
