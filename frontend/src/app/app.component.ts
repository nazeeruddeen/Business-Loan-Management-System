import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BusinessLoanApiService } from './business-loan-api.service';
import {
  ApplicationDecisionRequest,
  ApplicationStatus,
  BorrowerAddressRequest,
  BorrowerResponse,
  BusinessLoanDashboardResponse,
  CreateBorrowerRequest,
  CreateEligibilityRuleRequest,
  CreateLoanApplicationRequest,
  CreateLoanProductRequest,
  DisbursementReportResponse,
  DisburseLoanRequest,
  EligibilityEvaluationResponse,
  EligibilityRuleResponse,
  EvaluateEligibilityRequest,
  LoanAccountResponse,
  LoanApplicationResponse,
  LoanProductResponse,
  PaymentMode,
  RecordRepaymentRequest
} from './business-loan.models';

type NoticeKind = 'info' | 'success' | 'warning' | 'danger';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  readonly title = 'Business Loan Management System';
  readonly apiBaseUrl = 'http://localhost:8080/api/v1';
  readonly addressTypes: BorrowerAddressRequest['addressType'][] = ['REGISTERED', 'OPERATIONAL', 'CORRESPONDENCE'];
  readonly paymentModes: PaymentMode[] = ['CASH', 'UPI', 'NEFT', 'RTGS', 'CHEQUE', 'CARD', 'BANK_TRANSFER'];
  readonly applicationStatuses: ApplicationStatus[] = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'DISBURSED'];

  loading = false;
  notice: { kind: NoticeKind; text: string } = { kind: 'info', text: 'Ready' };
  summary: BusinessLoanDashboardResponse = {
    totalLoanApplications: 0,
    approvedLoanApplications: 0,
    disbursedLoanAccounts: 0,
    activeLoanAccounts: 0,
    overdueInstallments: 0,
    totalPrincipalDisbursed: 0,
    totalOutstandingPrincipal: 0,
    totalRepaidAmount: 0
  };

  borrowers: BorrowerResponse[] = [];
  loanProducts: LoanProductResponse[] = [];
  applications: LoanApplicationResponse[] = [];
  accounts: LoanAccountResponse[] = [];
  rules: EligibilityRuleResponse[] = [];
  report: DisbursementReportResponse | null = null;
  eligibility: EligibilityEvaluationResponse | null = null;
  selectedApplication: LoanApplicationResponse | null = null;
  selectedAccount: LoanAccountResponse | null = null;
  reportPage = 0;

  borrowerSearchForm = this.fb.group({
    businessPan: [''],
    businessName: ['']
  });

  borrowerForm = this.fb.group({
    legalBusinessName: ['', [Validators.required, Validators.maxLength(150)]],
    contactPersonName: ['', [Validators.required, Validators.maxLength(120)]],
    businessPan: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/)]],
    gstin: [''],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    industryType: ['', [Validators.required, Validators.maxLength(80)]],
    annualTurnover: [null, [Validators.required, Validators.min(1)]],
    monthlyIncome: [null, [Validators.required, Validators.min(1)]],
    addresses: this.fb.array([this.createAddressGroup('REGISTERED')])
  });

  loanProductSearchForm = this.fb.group({
    name: [''],
    active: [''],
    amount: [''],
    maxTenureMonths: ['']
  });

  loanProductForm = this.fb.group({
    productCode: ['', [Validators.required, Validators.maxLength(40)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    minAmount: [null, [Validators.required, Validators.min(1)]],
    maxAmount: [null, [Validators.required, Validators.min(1)]],
    interestRate: [null, [Validators.required, Validators.min(1)]],
    tenureMonths: [36, [Validators.required, Validators.min(1)]],
    eligibilityCriteria: [''],
    active: [true]
  });

  applicationSearchForm = this.fb.group({
    status: ['']
  });

  applicationForm = this.fb.group({
    borrowerId: [null, [Validators.required]],
    loanProductId: [null, [Validators.required]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]],
    purpose: ['', [Validators.required, Validators.maxLength(200)]]
  });

  reviewerForm = this.fb.group({
    reviewerUsername: ['reviewer01', [Validators.required]]
  });

  decisionForm = this.fb.group({
    approved: [true],
    remarks: ['Approved after policy review']
  });

  disbursementForm = this.fb.group({
    disbursementReference: ['DISB-2026-001', [Validators.required]],
    disbursementDate: [this.today(), [Validators.required]]
  });

  eligibilityForm = this.fb.group({
    borrowerId: [null, [Validators.required]],
    loanProductId: [null, [Validators.required]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]]
  });

  ruleForm = this.fb.group({
    ruleCode: ['INCOME_MIN', [Validators.required, Validators.maxLength(40)]],
    ruleExpression: ['Monthly income should meet policy threshold', [Validators.required, Validators.maxLength(250)]],
    ruleType: ['MIN_VALUE', [Validators.required]],
    minValue: [100000],
    maxValue: [null],
    ruleValueText: [''],
    active: [true]
  });

  repaymentForm = this.fb.group({
    amount: [null, [Validators.required, Validators.min(1)]],
    paymentMode: ['UPI', [Validators.required]],
    transactionReference: ['TXN-001', [Validators.required]],
    paymentDate: [this.today(), [Validators.required]],
    notes: ['']
  });

  reportForm = this.fb.group({
    from: [''],
    to: [''],
    size: [8]
  });

  constructor(private readonly fb: FormBuilder, private readonly api: BusinessLoanApiService) {}

  ngOnInit(): void {
    this.refreshAll();
  }

  get addressArray(): FormArray {
    return this.borrowerForm.get('addresses') as FormArray;
  }

  refreshAll(): void {
    this.loading = true;
    this.notice = { kind: 'info', text: 'Refreshing business loan data' };
    this.api.dashboard().subscribe((summary) => (this.summary = summary));
    this.api.borrowers(this.borrowerSearchForm.getRawValue()).subscribe((items) => {
      this.borrowers = items;
    });
    this.api.loanProducts(this.productFilters()).subscribe((items) => {
      this.loanProducts = items;
    });
    this.api.applications(this.applicationFilters()).subscribe((items) => {
      this.applications = items;
      if (items.length && !this.selectedApplication) {
        this.selectApplication(items[0]);
      }
    });
    this.api.loanAccounts().subscribe((items) => {
      this.accounts = items;
      if (items.length && !this.selectedAccount) {
        this.selectAccount(items[0]);
      }
    });
    this.api.eligibilityRules().subscribe((items) => (this.rules = items));
    this.loadReport();
    this.loading = false;
  }

  addAddress(): void {
    this.addressArray.push(this.createAddressGroup('OPERATIONAL'));
  }

  removeAddress(index: number): void {
    if (this.addressArray.length > 1) {
      this.addressArray.removeAt(index);
    }
  }

  createBorrower(): void {
    if (this.borrowerForm.invalid) {
      this.touch(this.borrowerForm);
      this.warn('Borrower form has validation errors');
      return;
    }
    const payload = this.borrowerForm.getRawValue() as CreateBorrowerRequest;
    this.api.createBorrower(payload).subscribe({
      next: (borrower) => {
        this.borrowers = [borrower, ...this.borrowers];
        this.borrowerForm.reset();
        this.addressArray.clear();
        this.addressArray.push(this.createAddressGroup('REGISTERED'));
        this.notice = { kind: 'success', text: `Borrower ${borrower.legalBusinessName} created` };
      },
      error: () => this.warn('Unable to create borrower')
    });
  }

  createLoanProduct(): void {
    if (this.loanProductForm.invalid) {
      this.touch(this.loanProductForm);
      this.warn('Loan product form has validation errors');
      return;
    }
    const payload = this.loanProductForm.getRawValue() as CreateLoanProductRequest;
    this.api.createLoanProduct(payload).subscribe({
      next: (product) => {
        this.loanProducts = [product, ...this.loanProducts];
        this.notice = { kind: 'success', text: `Loan product ${product.productCode} created` };
      },
      error: () => this.warn('Unable to create loan product')
    });
  }

  createApplication(): void {
    if (this.applicationForm.invalid) {
      this.touch(this.applicationForm);
      this.warn('Loan application form has validation errors');
      return;
    }
    const payload = this.applicationForm.getRawValue() as CreateLoanApplicationRequest;
    this.api.createLoanApplication(payload).subscribe({
      next: (application) => {
        this.applications = [application, ...this.applications];
        this.selectApplication(application);
        this.notice = { kind: 'success', text: `Application ${application.id} created` };
      },
      error: () => this.warn('Unable to create application')
    });
  }

  submitApplication(): void {
    if (!this.selectedApplication) {
      this.warn('Select an application first');
      return;
    }
    this.api.submitApplication(this.selectedApplication.id).subscribe({
      next: (application) => this.replaceApplication(application),
      error: () => this.warn('Unable to submit application')
    });
  }

  assignReviewer(): void {
    if (!this.selectedApplication) {
      this.warn('Select an application first');
      return;
    }
    this.api.assignReviewer(this.selectedApplication.id, this.reviewerForm.getRawValue()).subscribe({
      next: (application) => this.replaceApplication(application),
      error: () => this.warn('Unable to assign reviewer')
    });
  }

  decideApplication(): void {
    if (!this.selectedApplication) {
      this.warn('Select an application first');
      return;
    }
    const payload = this.decisionForm.getRawValue() as ApplicationDecisionRequest;
    this.api.decideApplication(this.selectedApplication.id, payload).subscribe({
      next: (application) => this.replaceApplication(application),
      error: () => this.warn('Unable to save decision')
    });
  }

  disburseApplication(): void {
    if (!this.selectedApplication) {
      this.warn('Select an application first');
      return;
    }
    const payload = this.disbursementForm.getRawValue();
    this.api.disburseApplication(this.selectedApplication.id, payload as DisburseLoanRequest).subscribe({
      next: (application) => {
        this.replaceApplication(application);
        this.notice = { kind: 'success', text: `Application ${application.id} disbursed` };
        this.loadReport();
        this.api.loanAccountByApplication(application.id).subscribe((account) => this.selectAccount(account));
      },
      error: () => this.warn('Unable to disburse application')
    });
  }

  evaluateEligibility(): void {
    if (this.eligibilityForm.invalid) {
      this.touch(this.eligibilityForm);
      this.warn('Eligibility form has validation errors');
      return;
    }
    this.api.evaluateEligibility(this.eligibilityForm.getRawValue() as EvaluateEligibilityRequest).subscribe((result) => {
      this.eligibility = result;
      this.notice = result.eligible
        ? { kind: 'success', text: result.summary }
        : { kind: 'warning', text: result.summary };
    });
  }

  createRule(): void {
    if (this.ruleForm.invalid) {
      this.touch(this.ruleForm);
      this.warn('Rule form has validation errors');
      return;
    }
    const payload = this.ruleForm.getRawValue() as CreateEligibilityRuleRequest;
    this.api.createEligibilityRule(payload).subscribe({
      next: (rule) => {
        this.rules = [rule, ...this.rules];
        this.notice = { kind: 'success', text: `Rule ${rule.ruleCode} created` };
      },
      error: () => this.warn('Unable to create eligibility rule')
    });
  }

  recordRepayment(): void {
    if (!this.selectedAccount) {
      this.warn('Select a loan account first');
      return;
    }
    if (this.repaymentForm.invalid) {
      this.touch(this.repaymentForm);
      this.warn('Repayment form has validation errors');
      return;
    }
    this.api.recordRepayment(this.selectedAccount.id, this.repaymentForm.getRawValue() as RecordRepaymentRequest).subscribe({
      next: (account) => {
        this.selectAccount(account);
        this.accounts = this.accounts.map((item) => (item.id === account.id ? account : item));
        this.notice = { kind: 'success', text: `Repayment recorded for ${account.accountNumber}` };
      },
      error: () => this.warn('Unable to record repayment')
    });
  }

  loadReport(): void {
    const filters = this.reportForm.getRawValue();
    this.api.report({ from: filters.from || undefined, to: filters.to || undefined }, this.reportPage, filters.size ?? 8)
      .subscribe((report) => (this.report = report));
  }

  exportCsv(): void {
    const filters = this.reportForm.getRawValue();
    this.api.exportDisbursements({ from: filters.from || undefined, to: filters.to || undefined }).subscribe((csv) => {
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'disbursement-report.csv';
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  searchBorrowers(): void {
    this.api.borrowers(this.borrowerSearchForm.getRawValue()).subscribe((items) => {
      this.borrowers = items;
      this.notice = { kind: 'info', text: `Loaded ${items.length} borrower record(s)` };
    });
  }

  searchProducts(): void {
    this.api.loanProducts(this.productFilters()).subscribe((items) => {
      this.loanProducts = items;
      this.notice = { kind: 'info', text: `Loaded ${items.length} product record(s)` };
    });
  }

  searchApplications(): void {
    this.api.applications(this.applicationFilters()).subscribe((items) => {
      this.applications = items;
      if (items.length) {
        this.selectApplication(items[0]);
      }
      this.notice = { kind: 'info', text: `Loaded ${items.length} application record(s)` };
    });
  }

  previousPage(): void {
    if (this.report && this.report.page > 0) {
      this.reportPage -= 1;
      this.loadReport();
    }
  }

  nextPage(): void {
    if (this.report && this.report.page + 1 < this.report.totalPages) {
      this.reportPage += 1;
      this.loadReport();
    }
  }

  selectApplication(application: LoanApplicationResponse): void {
    this.selectedApplication = application;
    this.applicationForm.patchValue({
      borrowerId: application.borrowerId,
      loanProductId: application.loanProductId,
      requestedAmount: application.requestedAmount,
      requestedTenureMonths: application.requestedTenureMonths,
      purpose: application.purpose
    });
    this.eligibilityForm.patchValue({
      borrowerId: application.borrowerId,
      loanProductId: application.loanProductId,
      requestedAmount: application.requestedAmount,
      requestedTenureMonths: application.requestedTenureMonths
    });
  }

  selectAccount(account: LoanAccountResponse): void {
    this.selectedAccount = account;
    this.repaymentForm.patchValue({
      amount: account.monthlyInstallmentAmount,
      transactionReference: `TXN-${account.accountNumber}`,
      paymentDate: this.today()
    });
  }

  formatMoney(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  private productFilters(): { name?: string; active?: boolean | null; amount?: number; maxTenureMonths?: number } {
    const value = this.loanProductSearchForm.getRawValue();
    return {
      name: value.name || undefined,
      active: value.active === '' ? null : value.active === 'true' ? true : value.active === 'false' ? false : null,
      amount: value.amount ? Number(value.amount) : undefined,
      maxTenureMonths: value.maxTenureMonths ? Number(value.maxTenureMonths) : undefined
    };
  }

  private applicationFilters(): { status?: string | null } {
    const value = this.applicationSearchForm.getRawValue();
    return {
      status: value.status || undefined
    };
  }

  private replaceApplication(application: LoanApplicationResponse): void {
    this.selectedApplication = application;
    this.applications = this.applications.map((item) => (item.id === application.id ? application : item));
    this.applications = [application, ...this.applications.filter((item) => item.id !== application.id)];
  }

  private createAddressGroup(addressType: BorrowerAddressRequest['addressType'] = 'REGISTERED') {
    return this.fb.group({
      addressType: [addressType, Validators.required],
      lineOne: ['', [Validators.required, Validators.maxLength(160)]],
      lineTwo: [''],
      city: ['', [Validators.required, Validators.maxLength(80)]],
      state: ['', [Validators.required, Validators.maxLength(80)]],
      postalCode: ['', [Validators.required, Validators.maxLength(15)]],
      country: ['', [Validators.required, Validators.maxLength(80)]]
    });
  }

  private touch(control: AbstractControl): void {
    control.markAllAsTouched();
  }

  private warn(text: string): void {
    this.notice = { kind: 'warning', text };
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
