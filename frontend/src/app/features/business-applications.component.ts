import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { EligibilityEvaluationResponse, LoanApplicationResponse } from '../business-loan.models';

@Component({
  selector: 'app-business-applications',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Eligibility and application draft flow</h2>
        <button type="button" class="ghost" (click)="searchApplications.emit()">Reload applications</button>
      </header>

      <div class="split-grid">
        <div>
          <form class="form" [formGroup]="eligibilityForm">
            <div class="row">
              <label>
                Borrower ID
                <input type="number" formControlName="borrowerId" min="1" data-testid="business-eligibility-borrower-id">
              </label>
              <label>
                Product ID
                <input type="number" formControlName="loanProductId" min="1" data-testid="business-eligibility-product-id">
              </label>
            </div>
            <div class="row">
              <label>
                Requested amount
                <input type="number" formControlName="requestedAmount" min="1" data-testid="business-eligibility-requested-amount">
              </label>
              <label>
                Tenure months
                <input type="number" formControlName="requestedTenureMonths" min="1" data-testid="business-eligibility-tenure">
              </label>
            </div>
            <button type="button" class="primary" (click)="evaluateEligibility.emit()" [disabled]="actionBusy === 'evaluateEligibility'" data-testid="business-evaluate-eligibility">
              {{ actionBusy === 'evaluateEligibility' ? 'Evaluating...' : 'Evaluate eligibility' }}
            </button>
          </form>

          <div class="result-box" *ngIf="eligibility">
            <strong [class.pass]="eligibility.eligible" [class.fail]="!eligibility.eligible">
              {{ eligibility.eligible ? 'Eligible' : 'Not eligible' }}
            </strong>
            <p>{{ eligibility.summary }}</p>
            <ul>
              <li *ngFor="let rule of eligibility.ruleResults">
                <span>{{ rule.ruleCode }}</span>
                <span>{{ rule.passed ? 'Pass' : 'Fail' }}</span>
                <small>{{ rule.message }}</small>
              </li>
            </ul>
          </div>
        </div>

        <form class="form" [formGroup]="applicationForm">
          <div class="row">
            <label>
              Borrower ID
              <input type="number" formControlName="borrowerId" min="1" data-testid="business-application-borrower-id">
            </label>
            <label>
              Product ID
              <input type="number" formControlName="loanProductId" min="1" data-testid="business-application-product-id">
            </label>
          </div>
          <div class="row">
            <label>
              Requested amount
              <input type="number" formControlName="requestedAmount" min="1" data-testid="business-application-requested-amount">
            </label>
            <label>
              Tenure months
              <input type="number" formControlName="requestedTenureMonths" min="1" data-testid="business-application-tenure">
            </label>
          </div>
          <label>
            Purpose
            <textarea rows="3" formControlName="purpose" data-testid="business-application-purpose"></textarea>
          </label>
          <button type="button" class="primary" (click)="createApplication.emit()" [disabled]="actionBusy === 'createApplication'" data-testid="business-create-application">
            {{ actionBusy === 'createApplication' ? 'Creating...' : 'Create application' }}
          </button>
        </form>
      </div>

      <div class="list">
        <article
          class="list-card list-card--selectable"
          *ngFor="let application of applications"
          (click)="selectApplication.emit(application)"
          [class.is-selected]="selectedApplication?.id === application.id"
          [attr.data-testid]="'business-application-card-' + application.id">
          <strong>{{ application.borrowerName }} · {{ application.loanProductCode }}</strong>
          <span>₹{{ formatMoney(application.requestedAmount) }} · {{ application.requestedTenureMonths }} months</span>
          <small>Status {{ application.status }} · {{ application.eligibilitySummary }}</small>
          <div class="chip-list">
            <span class="chip" [attr.data-kind]="application.borrowerKycComplete ? 'success' : 'warning'">
              {{ application.borrowerKycComplete ? 'KYC ready' : 'KYC incomplete' }}
            </span>
            <span class="chip" [attr.data-kind]="application.eligibilityPassed ? 'success' : 'danger'">
              {{ application.eligibilityPassed ? 'Eligibility passed' : 'Eligibility blocked' }}
            </span>
          </div>
        </article>
      </div>
    </article>
  `
})
export class BusinessApplicationsComponent {
  @Input({ required: true }) eligibilityForm!: FormGroup;
  @Input({ required: true }) applicationForm!: FormGroup;
  @Input({ required: true }) applications!: LoanApplicationResponse[];
  @Input() selectedApplication: LoanApplicationResponse | null = null;
  @Input() eligibility: EligibilityEvaluationResponse | null = null;
  @Input() actionBusy: string | null = null;

  @Output() searchApplications = new EventEmitter<void>();
  @Output() evaluateEligibility = new EventEmitter<void>();
  @Output() createApplication = new EventEmitter<void>();
  @Output() selectApplication = new EventEmitter<LoanApplicationResponse>();

  formatMoney(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }
}
