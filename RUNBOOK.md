# Business Loan Production Runbook

This runbook matches the current production hardening in the codebase:
- versioned eligibility rules
- KYC-gated submission and reviewer assignment
- borrower update flow with paginated search surfaces
- repayment servicing with principal-curtailment prepayment support
- structured `409 Conflict` and `422 Unprocessable Entity` workflow errors
- secured Angular operator console with visible workflow blockers
- structured logging and correlation-aware lifecycle actions
- ExternalSecret-backed runtime configuration and local-profile-gated bootstrap users

## Normal operating posture
- Use `admin` for platform administration.
- Use `officer` for borrower and application operations.
- Use `reviewer` for review and decision workflows.
- Use `borrower` for self-service viewing where permitted.

## Key surfaces
- Borrower and KYC APIs: `/api/v1/borrowers`
- Eligibility APIs: `/api/v1/eligibility` and `/api/v1/eligibility-rules`
- Application workflow APIs: `/api/v1/loan-applications`
- Accounts and servicing APIs: `/api/v1/loan-accounts`
- Reports: `/api/v1/reports/disbursements`

## What to watch
- Applications blocked by incomplete KYC
- `409 Conflict` from concurrent application updates
- `422 Unprocessable Entity` from business-rule violations
- Eligibility rule version changes
- Dashboard/report latency
- Disbursement and repayment workflow failures
- unexpected prepayment or schedule-recast behavior

## KYC and submit blocker handling
1. Check the application and borrower document state in the UI.
2. Confirm the missing required document type before retrying submit or reviewer assignment.
3. Do not bypass the blocker in the UI or API.
4. Reload the application if a `409 Conflict` indicates another operator changed state.

## Eligibility rule change handling
1. Verify the new rule version in the eligibility rule list.
2. Confirm the operator console is showing the current rule state.
3. Re-run eligibility evaluation after the policy change is deployed.
4. Preserve the old rule version in audit/history for traceability.

## Incident checklist
- Preserve correlation IDs and request IDs before any remediation.
- Do not mutate historical approval or disbursement decisions to hide a workflow error.
- Escalate to the workflow owner if a business-rule change causes widespread `422` responses.
- Treat unexpected `409 Conflict` spikes as a sign of concurrent operator activity or stale UI state.

## Deployment posture
- Production expects connection settings and JWT secrets from the cluster secret store, not committed YAML values.
- The backend is deployed as a two-replica rolling update target.
- External access is expected through the ingress manifest at `k8s/07-ingress.yaml`, with `/` routed to the frontend service and `/api` routed to the backend service.
- Replace the placeholder host `business-loan.example.com` and TLS secret `business-loan-tls` with platform-owned DNS and certificate values before live deployment.
- Keep Actuator endpoints internal to the cluster unless the platform team explicitly exposes them through a protected operations ingress.
- The in-repo MySQL manifest is for local or integration use; production should point at a managed HA MySQL service.

## Local verification
- Backend: `mvn clean test`
- Frontend: `npm run build`
- Full stack: `docker compose up -d --build`

## Playwright golden path
- Required env:
  - `BUSINESS_E2E_PASSWORD`
  - optional `BUSINESS_E2E_USERNAME` defaults to `admin`
  - optional `BUSINESS_E2E_BASE_URL` defaults to `http://127.0.0.1:4300`
  - optional `BUSINESS_E2E_API_BASE_URL` defaults to `http://127.0.0.1:8010`
- Start the local stack first with `docker compose up -d --build`.
- Then run `.\node_modules\.bin\playwright.cmd test` from `frontend`.
- The golden path covers login, borrower onboarding, KYC verification, loan application creation, reviewer assignment, and approval.

## Local observability
- Start the monitoring stack from `observability` with `docker compose up -d`.
- Prometheus: [http://localhost:9091](http://localhost:9091)
- Grafana: [http://localhost:3001](http://localhost:3001)
- Default Grafana credentials:
  - username `admin`
  - password `admin`
- The provisioned dashboard scrapes `host.docker.internal:8010/actuator/prometheus`.

## Minikube smoke deployment
- Build unique images such as `business-loan-management-system:smoke-1` and
  `business-loan-management-system-frontend:smoke-1`.
- Load those images with `minikube image load`.
- Create the smoke secret before backend rollout and ensure
  `APP_SECURITY_JWT_SECRET` is Base64-encoded.
- Set deployment images explicitly with `kubectl set image` instead of relying
  on `:latest`.
- Watch `business-loan-backend`, `business-loan-frontend`, `business-loan-mysql`,
  and `business-loan-redis` until they are healthy.
- Verify ingress from the ingress controller pod and expect:
  - `308 Permanent Redirect` on HTTP
  - `200 OK` plus frontend HTML on HTTPS
