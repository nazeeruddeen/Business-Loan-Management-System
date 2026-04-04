import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { BusinessLoanDashboardResponse } from '../business-loan.models';

@Component({
  selector: 'app-business-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="hero animated-panel">
      <div class="hero__copy">
        <div class="eyebrow">Live lending control room</div>
        <h2>Operational snapshot</h2>
        <p>
          The dashboard reflects live borrower onboarding, KYC readiness, application workflow progression, disbursement, and servicing outcomes from the backend.
        </p>
      </div>

      <div class="hero__metrics">
        <article class="metric">
          <span>Total applications</span>
          <strong>{{ summary.totalLoanApplications }}</strong>
        </article>
        <article class="metric">
          <span>Approved applications</span>
          <strong>{{ summary.approvedLoanApplications }}</strong>
        </article>
        <article class="metric">
          <span>Active accounts</span>
          <strong>{{ summary.activeLoanAccounts }}</strong>
        </article>
        <article class="metric">
          <span>Overdue installments</span>
          <strong>{{ summary.overdueInstallments }}</strong>
        </article>
      </div>
    </section>
  `
})
export class BusinessDashboardComponent {
  @Input({ required: true }) summary!: BusinessLoanDashboardResponse;
}
