# Business Loan Production Alignment

Product role
- flagship enterprise lending application in the workspace

Production hardening now owned by this repo
- actuator health endpoints are probe-safe
- frontend nginx runtime config is production-safe
- ingress manifest is in place for `/` and `/api`
- borrower KYC/document completeness blocks submit and reviewer assignment
- workflow errors surface as structured business responses
- dashboard and servicing paths use the hardened cache behavior now committed
- backend, frontend, Docker, Kubernetes, and ingress smoke validation were
  completed successfully in Minikube

Current repo-owned priorities
1. Keep the current full-stack runtime stable and demoable.
2. Preserve the KYC, eligibility, lifecycle, and servicing story as the main
   interview path.
3. Add only targeted operator visibility or audit improvements if they strengthen
   the demo without widening the maintenance surface.

Smoke validation status
- backend rollout: passed
- frontend rollout: passed
- ingress verification: passed

Best demo flow
1. Create or review a borrower with KYC/document state.
2. Show that incomplete KYC blocks submission or reviewer assignment.
3. Evaluate eligibility under the current rule version.
4. Progress the application through review, approval, and disbursement.
5. Show servicing, repayment posting, and reporting surfaces.
