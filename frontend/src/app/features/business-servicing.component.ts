import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DisbursementReportResponse, LoanAccountResponse, PaymentMode } from '../business-loan.models';

@Component({
  selector: 'app-business-servicing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styles: [`
    .report-toolbar {
      display: grid;
      grid-template-columns: minmax(0, 200px) 1fr;
      gap: 18px;
      align-items: end;
    }

    .report-toolbar__actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      justify-content: flex-start;
      align-items: center;
      padding-bottom: 2px;
    }

    .report-toolbar__actions button {
      min-width: 108px;
    }

    @media (max-width: 900px) {
      .report-toolbar {
        grid-template-columns: 1fr;
      }

      .report-toolbar__actions {
        justify-content: stretch;
      }

      .report-toolbar__actions button {
        flex: 1 1 160px;
      }
    }
  `],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Loan servicing and disbursement reporting</h2>
        <div class="chip" *ngIf="selectedAccount">Account {{ selectedAccount.accountNumber }}</div>
      </header>

      <div class="split-grid">
        <form class="form" [formGroup]="repaymentForm">
          <div class="section-subtitle">Repayment capture</div>
          <div class="row">
            <label>
              Amount
              <input type="number" formControlName="amount" min="1">
            </label>
            <label>
              Payment mode
              <select formControlName="paymentMode">
                <option *ngFor="let mode of paymentModes" [value]="mode">{{ mode }}</option>
              </select>
            </label>
          </div>
          <div class="row">
            <label>
              Transaction reference
              <input type="text" formControlName="transactionReference">
            </label>
            <label>
              Payment date
              <input type="date" formControlName="paymentDate">
            </label>
          </div>
          <label>
            Notes
            <textarea rows="3" formControlName="notes"></textarea>
          </label>
          <button type="button" class="primary" (click)="recordRepayment.emit()" [disabled]="!selectedAccount || actionBusy === 'recordRepayment'">
            {{ actionBusy === 'recordRepayment' ? 'Recording...' : 'Record repayment' }}
          </button>
        </form>

        <div class="report">
          <div class="section-subtitle">Disbursement report</div>
          <form class="form" [formGroup]="reportForm">
            <div class="row">
              <label>
                From
                <input type="date" formControlName="from">
              </label>
              <label>
                To
                <input type="date" formControlName="to">
              </label>
            </div>
            <div class="report-toolbar">
              <label>
                Page size
                <input type="number" formControlName="size" min="1">
              </label>
              <div class="report-toolbar__actions">
                <button type="button" class="ghost" (click)="loadReport.emit()">Load</button>
                <button type="button" class="ghost" (click)="previousPage.emit()">Prev</button>
                <button type="button" class="ghost" (click)="nextPage.emit()">Next</button>
                <button type="button" class="ghost" (click)="exportCsv.emit()" [disabled]="actionBusy === 'exportCsv'">
                  {{ actionBusy === 'exportCsv' ? 'Exporting...' : 'Export CSV' }}
                </button>
              </div>
            </div>
          </form>

          <div class="report-summary" *ngIf="report">
            <article>
              <span>Disbursed count</span>
              <strong>{{ report.disbursedCount }}</strong>
            </article>
            <article>
              <span>Total principal</span>
              <strong>₹{{ formatMoney(report.totalPrincipalDisbursed) }}</strong>
            </article>
            <article>
              <span>Outstanding</span>
              <strong>₹{{ formatMoney(report.totalOutstandingPrincipal) }}</strong>
            </article>
          </div>
        </div>
      </div>

      <div class="list">
        <article
          class="list-card list-card--selectable"
          *ngFor="let account of accounts"
          (click)="selectAccount.emit(account)"
          [class.is-selected]="selectedAccount?.id === account.id">
          <strong>{{ account.accountNumber }} · {{ account.borrowerName }}</strong>
          <span>₹{{ formatMoney(account.principalAmount) }} · Outstanding ₹{{ formatMoney(account.outstandingPrincipal) }}</span>
          <small>{{ account.status }} · Next due {{ account.nextDueDate || 'N/A' }}</small>
        </article>
      </div>

      <div class="list" *ngIf="report?.items?.length">
        <article class="list-card" *ngFor="let item of report?.items">
          <strong>{{ item.accountNumber }} · {{ item.borrowerName }}</strong>
          <span>{{ item.productCode }} · ₹{{ formatMoney(item.principalAmount) }}</span>
          <small>{{ item.status }} · Disbursed {{ item.disbursedAt | date:'mediumDate' }}</small>
        </article>
      </div>
    </article>
  `
})
export class BusinessServicingComponent {
  @Input({ required: true }) repaymentForm!: FormGroup;
  @Input({ required: true }) reportForm!: FormGroup;
  @Input({ required: true }) paymentModes!: PaymentMode[];
  @Input({ required: true }) accounts!: LoanAccountResponse[];
  @Input() selectedAccount: LoanAccountResponse | null = null;
  @Input() report: DisbursementReportResponse | null = null;
  @Input() actionBusy: string | null = null;

  @Output() recordRepayment = new EventEmitter<void>();
  @Output() loadReport = new EventEmitter<void>();
  @Output() previousPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();
  @Output() exportCsv = new EventEmitter<void>();
  @Output() selectAccount = new EventEmitter<LoanAccountResponse>();

  formatMoney(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }
}
