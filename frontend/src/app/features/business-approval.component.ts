import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { LoanApplicationResponse, UserSummaryResponse } from '../business-loan.models';

@Component({
  selector: 'app-business-approval',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Approval routing and disbursement</h2>
        <div class="chip" *ngIf="selectedApplication">Selected #{{ selectedApplication.id }}</div>
      </header>

      <div class="selected" *ngIf="selectedApplication">
        <div class="selected__header">
          <strong>{{ selectedApplication.borrowerName }}</strong>
          <span data-testid="business-selected-status">{{ selectedApplication.status }}</span>
        </div>
        <p>{{ selectedApplication.purpose }}</p>
        <small>KYC ready: {{ selectedApplication.borrowerKycComplete ? 'Yes' : 'No' }}</small>
      </div>

      <div class="selected">
        <div class="selected__header">
          <strong>{{ workflowStatusLabel(selectedApplication) }}</strong>
          <span class="chip" [attr.data-kind]="workflowStatusTone(selectedApplication)">
            {{ workflowBlockers(selectedApplication).length ? 'Blocked' : 'Ready' }}
          </span>
        </div>
        <p *ngIf="workflowBlockers(selectedApplication).length; else readyToProceed">
          {{ workflowBlockers(selectedApplication).join(' | ') }}
        </p>
        <ng-template #readyToProceed>
          <p>This application can move through the workflow without KYC or eligibility blockers.</p>
        </ng-template>
      </div>

      <div class="split-grid">
        <form class="form" [formGroup]="reviewerForm">
          <div class="section-subtitle">Submission and reviewer assignment</div>
          <button type="button" class="secondary" (click)="submitApplication.emit()" [disabled]="!canSubmit(selectedApplication) || actionBusy === 'submitApplication'" data-testid="business-submit-application">
            {{ actionBusy === 'submitApplication' ? 'Submitting...' : 'Submit application' }}
          </button>
          <label>
            Reviewer
            <select formControlName="reviewerUserId" data-testid="business-reviewer-select">
              <option value="">Select reviewer</option>
              <option *ngFor="let reviewer of reviewers" [value]="reviewer.id">
                {{ reviewer.username }} ({{ reviewer.role }})
              </option>
            </select>
          </label>
          <button type="button" class="secondary" (click)="assignReviewer.emit()" [disabled]="!canAssignReviewer(selectedApplication) || actionBusy === 'assignReviewer'" data-testid="business-assign-reviewer">
            {{ actionBusy === 'assignReviewer' ? 'Assigning...' : 'Assign reviewer' }}
          </button>
        </form>

        <div>
          <form class="form" [formGroup]="decisionForm">
            <div class="section-subtitle">Decision</div>
            <div class="row">
              <label>
                Decision
                <select formControlName="decisionStatus" data-testid="business-decision-status">
                  <option *ngFor="let status of decisionStatuses" [value]="status">{{ status }}</option>
                </select>
              </label>
              <label>
                Remarks
                <input type="text" formControlName="remarks" data-testid="business-decision-remarks">
              </label>
            </div>
            <button type="button" class="secondary" (click)="decideApplication.emit()" [disabled]="!selectedApplication || actionBusy === 'decideApplication'" data-testid="business-save-decision">
              {{ actionBusy === 'decideApplication' ? 'Saving...' : 'Save decision' }}
            </button>
          </form>

          <form class="form" [formGroup]="disbursementForm">
            <div class="section-subtitle">Disbursement</div>
            <div class="row">
              <label>
                Disbursement reference
                <input type="text" formControlName="disbursementReference">
              </label>
              <label>
                Disbursement date
                <input type="date" formControlName="disbursementDate">
              </label>
            </div>
            <button type="button" class="primary" (click)="disburseApplication.emit()" [disabled]="!selectedApplication || actionBusy === 'disburseApplication'">
              {{ actionBusy === 'disburseApplication' ? 'Disbursing...' : 'Disburse application' }}
            </button>
          </form>
        </div>
      </div>

      <div class="history-list" *ngIf="selectedApplication?.history?.length">
        <div class="section-subtitle">Workflow history</div>
        <article class="timeline-card" *ngFor="let item of selectedApplication?.history">
          <strong>{{ item.fromStatus || 'START' }} → {{ item.toStatus }}</strong>
          <span>{{ item.remarks || 'Status updated' }}</span>
          <small>{{ item.changedBy }} · {{ item.changedAt | date:'medium' }}</small>
        </article>
      </div>
    </article>
  `
})
export class BusinessApprovalComponent {
  @Input() selectedApplication: LoanApplicationResponse | null = null;
  @Input({ required: true }) reviewers!: UserSummaryResponse[];
  @Input({ required: true }) reviewerForm!: FormGroup;
  @Input({ required: true }) decisionForm!: FormGroup;
  @Input({ required: true }) disbursementForm!: FormGroup;
  @Input({ required: true }) decisionStatuses!: Array<'APPROVED' | 'REJECTED'>;
  @Input() actionBusy: string | null = null;

  @Output() submitApplication = new EventEmitter<void>();
  @Output() assignReviewer = new EventEmitter<void>();
  @Output() decideApplication = new EventEmitter<void>();
  @Output() disburseApplication = new EventEmitter<void>();

  workflowBlockers(application: LoanApplicationResponse | null): string[] {
    if (!application) {
      return [];
    }

    const blockers: string[] = [];
    if (!application.borrowerKycComplete) {
      const missing = application.missingRequiredDocuments.length
        ? `Missing verified documents: ${application.missingRequiredDocuments.join(', ')}`
        : 'KYC is incomplete';
      blockers.push(missing);
    }
    if (!application.eligibilityPassed) {
      blockers.push(application.eligibilitySummary || 'Eligibility checks have failed');
    }
    return blockers;
  }

  workflowStatusLabel(application: LoanApplicationResponse | null): string {
    if (!application) {
      return 'No application selected';
    }
    return this.workflowBlockers(application).length ? 'Workflow blocked' : 'Ready for workflow progression';
  }

  workflowStatusTone(application: LoanApplicationResponse | null): 'info' | 'success' | 'warning' {
    if (!application) {
      return 'info';
    }
    return this.workflowBlockers(application).length ? 'warning' : 'success';
  }

  canSubmit(application: LoanApplicationResponse | null): boolean {
    return !!application
      && application.status === 'DRAFT'
      && application.borrowerKycComplete
      && application.eligibilityPassed;
  }

  canAssignReviewer(application: LoanApplicationResponse | null): boolean {
    return !!application
      && application.status === 'SUBMITTED'
      && application.borrowerKycComplete;
  }
}
