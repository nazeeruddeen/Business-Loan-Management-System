import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, forkJoin, of } from 'rxjs';
import { AuthSessionService } from './auth-session.service';
import { BusinessLoanApiService } from './business-loan-api.service';
import { environment } from '../environments/environment';
import {
  ApiErrorResponse,
  ApplicationDecisionRequest,
  ApplicationStatus,
  AssignReviewerRequest,
  AuthResponse,
  BorrowerAddressRequest,
  BorrowerDocumentResponse,
  BorrowerDocumentStatus,
  BorrowerDocumentType,
  BorrowerResponse,
  BusinessLoanDashboardResponse,
  CreateBorrowerDocumentRequest,
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
  LoginRequest,
  PagedResponse,
  PaymentMode,
  RecordRepaymentRequest,
  UpdateBorrowerDocumentStatusRequest,
  UserInfoResponse,
  UserSummaryResponse
} from './business-loan.models';

type NoticeKind = 'info' | 'success' | 'warning' | 'danger';
type AppTab = 'dashboard' | 'borrowers' | 'products' | 'applications' | 'approval' | 'servicing';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  readonly title = 'Business Loan Management System';
  readonly apiBaseUrl = environment.apiBaseUrl;
  readonly addressTypes: BorrowerAddressRequest['addressType'][] = ['REGISTERED', 'OPERATIONAL', 'CORRESPONDENCE'];
  readonly paymentModes: PaymentMode[] = ['CASH', 'UPI', 'NEFT', 'RTGS', 'CHEQUE', 'CARD', 'BANK_TRANSFER'];
  readonly applicationStatuses: ApplicationStatus[] = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'DISBURSED'];
  readonly decisionStatuses: Array<'APPROVED' | 'REJECTED'> = ['APPROVED', 'REJECTED'];
  readonly documentTypes: BorrowerDocumentType[] = [
    'PAN_CARD',
    'BUSINESS_REGISTRATION',
    'BANK_STATEMENT',
    'GST_CERTIFICATE',
    'ITR',
    'ADDRESS_PROOF',
    'OTHER'
  ];

  activeTab: AppTab = 'dashboard';
  bootstrapping = false;
  pageBusy = false;
  actionBusy: string | null = null;
  notice: { kind: NoticeKind; text: string } = { kind: 'info', text: 'Sign in to load the live business lending workspace.' };

  currentUser: UserInfoResponse | null = null;
  authResponse: AuthResponse | null = null;
  reviewers: UserSummaryResponse[] = [];

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
  borrowerDocuments: BorrowerDocumentResponse[] = [];
  loanProducts: LoanProductResponse[] = [];
  applications: LoanApplicationResponse[] = [];
  accounts: LoanAccountResponse[] = [];
  rules: EligibilityRuleResponse[] = [];
  report: DisbursementReportResponse | null = null;
  eligibility: EligibilityEvaluationResponse | null = null;
  selectedBorrower: BorrowerResponse | null = null;
  selectedApplication: LoanApplicationResponse | null = null;
  selectedAccount: LoanAccountResponse | null = null;
  reportPage = 0;
  readonly listPageSize = 20;

  authForm = this.fb.group<any>({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  borrowerSearchForm = this.fb.group<any>({
    businessPan: [''],
    businessName: ['']
  });

  borrowerForm = this.fb.group<any>({
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

  documentForm = this.fb.group<any>({
    documentType: ['PAN_CARD', [Validators.required]],
    fileName: ['', [Validators.required, Validators.maxLength(180)]],
    fileReference: ['', [Validators.required, Validators.maxLength(255)]],
    remarks: ['']
  });

  documentReviewForm = this.fb.group<any>({
    remarks: ['']
  });

  loanProductSearchForm = this.fb.group<any>({
    name: [''],
    active: [''],
    amount: [''],
    maxTenureMonths: ['']
  });

  loanProductForm = this.fb.group<any>({
    productCode: ['', [Validators.required, Validators.maxLength(40)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    minAmount: [null, [Validators.required, Validators.min(1)]],
    maxAmount: [null, [Validators.required, Validators.min(1)]],
    interestRate: [null, [Validators.required, Validators.min(0.1)]],
    tenureMonths: [36, [Validators.required, Validators.min(1)]],
    eligibilityCriteria: [''],
    active: [true]
  });

  applicationSearchForm = this.fb.group<any>({
    status: ['']
  });

  applicationForm = this.fb.group<any>({
    borrowerId: [null, [Validators.required]],
    loanProductId: [null, [Validators.required]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]],
    purpose: ['', [Validators.required, Validators.maxLength(200)]]
  });

  reviewerForm = this.fb.group<any>({
    reviewerUserId: [null, [Validators.required]]
  });

  decisionForm = this.fb.group<any>({
    decisionStatus: ['APPROVED', [Validators.required]],
    remarks: ['', [Validators.required, Validators.maxLength(250)]]
  });

  disbursementForm = this.fb.group<any>({
    disbursementReference: ['', [Validators.required]],
    disbursementDate: [this.today(), [Validators.required]]
  });

  eligibilityForm = this.fb.group<any>({
    borrowerId: [null, [Validators.required]],
    loanProductId: [null, [Validators.required]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]]
  });

  ruleForm = this.fb.group<any>({
    ruleCode: ['INCOME_MIN', [Validators.required, Validators.maxLength(40)]],
    ruleExpression: ['Monthly income should meet policy threshold', [Validators.required, Validators.maxLength(250)]],
    ruleType: ['MIN_VALUE', [Validators.required]],
    minValue: [100000],
    maxValue: [null],
    ruleValueText: [''],
    active: [true]
  });

  repaymentForm = this.fb.group<any>({
    amount: [null, [Validators.required, Validators.min(1)]],
    paymentMode: ['UPI', [Validators.required]],
    transactionReference: ['', [Validators.required]],
    paymentDate: [this.today(), [Validators.required]],
    notes: ['']
  });

  reportForm = this.fb.group<any>({
    from: [''],
    to: [''],
    size: [8]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: BusinessLoanApiService,
    private readonly authSession: AuthSessionService
  ) {}

  ngOnInit(): void {
    if (this.authSession.isAuthenticated) {
      this.restoreSession();
    }
  }

  get addressArray(): FormArray {
    return this.borrowerForm.get('addresses') as FormArray;
  }

  get isAuthenticated(): boolean {
    return this.authSession.isAuthenticated;
  }

  get reportPageSize(): number {
    const size = Number(this.reportForm.get('size')?.value ?? 8);
    return Number.isFinite(size) && size > 0 ? size : 8;
  }

  setTab(tab: AppTab): void {
    this.activeTab = tab;
  }

  login(): void {
    if (this.authForm.invalid) {
      this.touch(this.authForm);
      this.notice = { kind: 'warning', text: 'Enter username and password to sign in.' };
      return;
    }

    this.actionBusy = 'login';
    const payload = this.authForm.getRawValue() as unknown as LoginRequest;
    this.api.login(payload).subscribe({
      next: (response) => {
        this.authSession.setSession(response);
        this.authResponse = response;
        this.currentUser = { username: response.username, role: response.role };
        this.notice = { kind: 'success', text: `Signed in as ${response.username}. Loading live lending data.` };
        this.actionBusy = null;
        this.refreshAll(true);
      },
      error: (error) => {
        this.actionBusy = null;
        this.handleError(error, 'Unable to sign in');
      }
    });
  }

  logout(): void {
    const refreshToken = this.authSession.session?.refreshToken;
    this.actionBusy = 'logout';
    const request = refreshToken ? this.api.logout(refreshToken) : of(void 0);
    request.subscribe({
      next: () => this.clearSession('Signed out from the business lending workspace.'),
      error: () => this.clearSession('Session cleared locally after logout attempt.')
    });
  }

  refreshAll(resetSelections = false): void {
    if (!this.isAuthenticated) {
      return;
    }

    this.pageBusy = true;
    this.notice = { kind: 'info', text: 'Refreshing dashboard, KYC queue, workflow states, and reports.' };

    const reviewers$ = this.currentUser?.role === 'ADMIN' ? this.api.users() : of([] as UserSummaryResponse[]);

    forkJoin({
      me: this.api.me(),
      summary: this.api.dashboard(),
      borrowers: this.api.borrowers(this.borrowerFilters()),
      products: this.api.loanProducts(this.productFilters()),
      applications: this.api.applications(this.applicationFilters()),
      accounts: this.api.loanAccounts(0, this.listPageSize),
      rules: this.api.eligibilityRules(),
      reviewers: reviewers$,
      report: this.api.report(this.reportFilters(), this.reportPage, this.reportPageSize)
    }).subscribe({
      next: ({ me, summary, borrowers, products, applications, accounts, rules, reviewers, report }) => {
        this.currentUser = me;
        this.authSession.setUserInfo(me);
        this.summary = summary;
        this.borrowers = borrowers.items;
        this.loanProducts = products;
        this.applications = applications.items;
        this.accounts = accounts.items;
        this.rules = rules;
        this.reviewers = reviewers.filter((user) => user.role === 'REVIEWER' && user.active);
        this.report = report;

        const borrowerId = resetSelections ? borrowers.items[0]?.id : this.selectedBorrower?.id ?? borrowers.items[0]?.id;
        const applicationId = resetSelections ? applications.items[0]?.id : this.selectedApplication?.id ?? applications.items[0]?.id;
        const accountId = resetSelections ? accounts.items[0]?.id : this.selectedAccount?.id ?? accounts.items[0]?.id;

        this.selectedBorrower = borrowers.items.find((item) => item.id === borrowerId) ?? borrowers.items[0] ?? null;
        this.selectedApplication = applications.items.find((item) => item.id === applicationId) ?? applications.items[0] ?? null;
        this.selectedAccount = accounts.items.find((item) => item.id === accountId) ?? accounts.items[0] ?? null;

        if (this.selectedBorrower) {
          this.loadBorrowerDocuments(this.selectedBorrower.id);
          this.applicationForm.patchValue({ borrowerId: this.selectedBorrower.id });
          this.eligibilityForm.patchValue({ borrowerId: this.selectedBorrower.id });
        } else {
          this.borrowerDocuments = [];
        }

        if (this.selectedApplication) {
          this.patchApplicationForms(this.selectedApplication);
        }

        if (this.selectedAccount) {
          this.patchRepaymentForm(this.selectedAccount);
        }

        this.pageBusy = false;
        this.notice = { kind: 'success', text: 'Live business lending data refreshed successfully.' };
      },
      error: (error) => {
        this.pageBusy = false;
        this.handleError(error, 'Unable to refresh the business lending workspace');
      }
    });
  }

  searchBorrowers(): void {
    this.api.borrowers(this.borrowerFilters()).subscribe({
      next: (borrowers) => {
        this.borrowers = borrowers.items;
        if (borrowers.items.length) {
          this.selectBorrower(borrowers.items[0]);
        } else {
          this.selectedBorrower = null;
          this.borrowerDocuments = [];
        }
        this.notice = { kind: 'info', text: `Loaded ${borrowers.items.length} borrower record(s) from ${borrowers.totalElements} total.` };
      },
      error: (error) => this.handleError(error, 'Unable to load borrowers')
    });
  }

  searchProducts(): void {
    this.api.loanProducts(this.productFilters()).subscribe({
      next: (products) => {
        this.loanProducts = products;
        this.notice = { kind: 'info', text: `Loaded ${products.length} product record(s).` };
      },
      error: (error) => this.handleError(error, 'Unable to load loan products')
    });
  }

  searchApplications(): void {
    this.api.applications(this.applicationFilters()).subscribe({
      next: (applications) => {
        this.applications = applications.items;
        if (applications.items.length) {
          this.selectApplication(applications.items[0]);
        } else {
          this.selectedApplication = null;
        }
        this.notice = { kind: 'info', text: `Loaded ${applications.items.length} application record(s) from ${applications.totalElements} total.` };
      },
      error: (error) => this.handleError(error, 'Unable to load applications')
    });
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
      this.notice = { kind: 'warning', text: 'Borrower form has validation errors.' };
      return;
    }

    this.runAction(
      'createBorrower',
      () => this.api.createBorrower(this.borrowerForm.getRawValue() as unknown as CreateBorrowerRequest),
      (borrower) => {
        this.borrowerForm.reset();
        this.addressArray.clear();
        this.addressArray.push(this.createAddressGroup('REGISTERED'));
        this.borrowers = [borrower, ...this.borrowers.filter((item) => item.id !== borrower.id)];
        this.selectBorrower(borrower);
        this.notice = { kind: 'success', text: `Borrower ${borrower.legalBusinessName} created.` };
      },
      'Unable to create borrower'
    );
  }

  selectBorrower(borrower: BorrowerResponse): void {
    this.selectedBorrower = borrower;
    this.applicationForm.patchValue({ borrowerId: borrower.id });
    this.eligibilityForm.patchValue({ borrowerId: borrower.id });
    this.loadBorrowerDocuments(borrower.id);
    this.notice = { kind: 'info', text: `Borrower ${borrower.legalBusinessName} selected for KYC actions.` };
  }

  createBorrowerDocument(): void {
    if (!this.selectedBorrower) {
      this.notice = { kind: 'warning', text: 'Select a borrower first.' };
      return;
    }
    if (this.documentForm.invalid) {
      this.touch(this.documentForm);
      this.notice = { kind: 'warning', text: 'Document form has validation errors.' };
      return;
    }

    const payload = this.documentForm.getRawValue() as unknown as CreateBorrowerDocumentRequest;
    this.runAction(
      'createDocument',
      () => this.api.createBorrowerDocument(this.selectedBorrower!.id, payload),
      () => {
        this.documentForm.patchValue({ fileName: '', fileReference: '', remarks: '' });
        this.reloadBorrowersAndDocuments(this.selectedBorrower!.id, 'Document metadata added to the borrower KYC queue.');
      },
      'Unable to add borrower document'
    );
  }

  reviewDocument(document: BorrowerDocumentResponse, documentStatus: BorrowerDocumentStatus): void {
    if (!this.selectedBorrower) {
      return;
    }

    const payload: UpdateBorrowerDocumentStatusRequest = {
      documentStatus,
      remarks: String(this.documentReviewForm.get('remarks')?.value ?? '')
    };

    this.runAction(
      `reviewDocument-${document.id}`,
      () => this.api.updateBorrowerDocumentStatus(this.selectedBorrower!.id, document.id, payload),
      () => {
        this.documentReviewForm.patchValue({ remarks: '' });
        this.reloadBorrowersAndDocuments(this.selectedBorrower!.id, `Document ${document.fileName} marked as ${documentStatus}.`);
      },
      'Unable to update borrower document status'
    );
  }

  createLoanProduct(): void {
    if (this.loanProductForm.invalid) {
      this.touch(this.loanProductForm);
      this.notice = { kind: 'warning', text: 'Loan product form has validation errors.' };
      return;
    }

    this.runAction(
      'createProduct',
      () => this.api.createLoanProduct(this.loanProductForm.getRawValue() as unknown as CreateLoanProductRequest),
      (product) => {
        this.loanProducts = [product, ...this.loanProducts.filter((item) => item.id !== product.id)];
        this.notice = { kind: 'success', text: `Loan product ${product.productCode} created.` };
      },
      'Unable to create loan product'
    );
  }

  evaluateEligibility(): void {
    if (this.eligibilityForm.invalid) {
      this.touch(this.eligibilityForm);
      this.notice = { kind: 'warning', text: 'Eligibility form has validation errors.' };
      return;
    }

    this.runAction(
      'evaluateEligibility',
      () => this.api.evaluateEligibility(this.eligibilityForm.getRawValue() as unknown as EvaluateEligibilityRequest),
      (result) => {
        this.eligibility = result;
        this.notice = { kind: result.eligible ? 'success' : 'warning', text: result.summary };
      },
      'Unable to evaluate eligibility'
    );
  }

  createApplication(): void {
    if (this.applicationForm.invalid) {
      this.touch(this.applicationForm);
      this.notice = { kind: 'warning', text: 'Loan application form has validation errors.' };
      return;
    }

    this.runAction(
      'createApplication',
      () => this.api.createLoanApplication(this.applicationForm.getRawValue() as unknown as CreateLoanApplicationRequest),
      (application) => {
        this.applications = [application, ...this.applications.filter((item) => item.id !== application.id)];
        this.selectApplication(application);
        this.reloadDashboard();
        this.notice = { kind: 'success', text: `Application ${application.id} created in draft state.` };
      },
      'Unable to create application'
    );
  }

  selectApplication(application: LoanApplicationResponse): void {
    this.selectedApplication = application;
    this.patchApplicationForms(application);
    this.notice = { kind: 'info', text: `Application ${application.id} selected for workflow actions.` };
  }

  submitApplication(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }

    this.runAction(
      'submitApplication',
      () => this.api.submitApplication(this.selectedApplication!.id),
      (application) => {
        this.replaceApplication(application);
        this.reloadDashboard();
        this.notice = { kind: 'success', text: `Application ${application.id} submitted for review.` };
      },
      'Unable to submit application'
    );
  }

  assignReviewer(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }
    if (this.reviewerForm.invalid) {
      this.touch(this.reviewerForm);
      this.notice = { kind: 'warning', text: 'Choose a reviewer before assigning.' };
      return;
    }

    this.runAction(
      'assignReviewer',
      () => this.api.assignReviewer(this.selectedApplication!.id, this.reviewerForm.getRawValue() as unknown as AssignReviewerRequest),
      (application) => {
        this.replaceApplication(application);
        this.reloadDashboard();
        this.notice = { kind: 'success', text: `Reviewer assigned to application ${application.id}.` };
      },
      'Unable to assign reviewer'
    );
  }

  decideApplication(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }
    if (this.decisionForm.invalid) {
      this.touch(this.decisionForm);
      this.notice = { kind: 'warning', text: 'Decision form has validation errors.' };
      return;
    }

    this.runAction(
      'decideApplication',
      () => this.api.decideApplication(this.selectedApplication!.id, this.decisionForm.getRawValue() as unknown as ApplicationDecisionRequest),
      (application) => {
        this.replaceApplication(application);
        this.reloadDashboard();
        this.notice = { kind: 'success', text: `Decision saved for application ${application.id}.` };
      },
      'Unable to save application decision'
    );
  }

  disburseApplication(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }
    if (this.disbursementForm.invalid) {
      this.touch(this.disbursementForm);
      this.notice = { kind: 'warning', text: 'Disbursement form has validation errors.' };
      return;
    }

    this.runAction(
      'disburseApplication',
      () => this.api.disburseApplication(this.selectedApplication!.id, this.disbursementForm.getRawValue() as unknown as DisburseLoanRequest),
      (application) => {
        this.replaceApplication(application);
        this.reloadDashboard();
        this.reloadAccounts(application.id);
        this.loadReport();
        this.notice = { kind: 'success', text: `Application ${application.id} disbursed successfully.` };
      },
      'Unable to disburse application'
    );
  }

  createRule(): void {
    if (this.ruleForm.invalid) {
      this.touch(this.ruleForm);
      this.notice = { kind: 'warning', text: 'Rule form has validation errors.' };
      return;
    }

    this.runAction(
      'createRule',
      () => this.api.createEligibilityRule(this.ruleForm.getRawValue() as unknown as CreateEligibilityRuleRequest),
      (rule) => {
        this.rules = [rule, ...this.rules.filter((item) => item.id !== rule.id)];
        this.notice = { kind: 'success', text: `Eligibility rule ${rule.ruleCode} created.` };
      },
      'Unable to create eligibility rule'
    );
  }

  selectAccount(account: LoanAccountResponse): void {
    this.selectedAccount = account;
    this.patchRepaymentForm(account);
    this.notice = { kind: 'info', text: `Account ${account.accountNumber} selected for servicing.` };
  }

  recordRepayment(): void {
    if (!this.selectedAccount) {
      this.notice = { kind: 'warning', text: 'Select a loan account first.' };
      return;
    }
    if (this.repaymentForm.invalid) {
      this.touch(this.repaymentForm);
      this.notice = { kind: 'warning', text: 'Repayment form has validation errors.' };
      return;
    }

    const account = this.selectedAccount;
    this.runAction(
      'recordRepayment',
      () => this.api.recordRepayment(account.id, this.repaymentForm.getRawValue() as unknown as RecordRepaymentRequest),
      (transaction) => {
        this.notice = { kind: 'success', text: `Repayment ${transaction.transactionReference} recorded successfully.` };
        this.reloadAccounts(account.applicationId);
        this.reloadDashboard();
        this.loadReport();
      },
      'Unable to record repayment'
    );
  }

  loadReport(): void {
    this.api.report(this.reportFilters(), this.reportPage, this.reportPageSize).subscribe({
      next: (report) => {
        this.report = report;
      },
      error: (error) => this.handleError(error, 'Unable to load disbursement report')
    });
  }

  exportCsv(): void {
    this.runAction(
      'exportCsv',
      () => this.api.exportDisbursements(this.reportFilters()),
      (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'disbursement-report.csv';
        link.click();
        URL.revokeObjectURL(url);
        this.notice = { kind: 'success', text: 'Disbursement report download started.' };
      },
      'Unable to export disbursement report'
    );
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

  formatMoney(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  formatLabel(value: string): string {
    return value.replace(/_/g, ' ');
  }

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
    const blockers = this.workflowBlockers(application);
    if (!blockers.length) {
      return 'Ready for workflow progression';
    }
    return 'Workflow blocked';
  }

  workflowStatusTone(application: LoanApplicationResponse | null): NoticeKind {
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

  kycBadgeKind(borrower: BorrowerResponse | null): NoticeKind {
    if (!borrower) {
      return 'info';
    }
    return borrower.kycSummary.kycComplete ? 'success' : 'warning';
  }

  documentCanVerify(document: BorrowerDocumentResponse): boolean {
    return document.documentStatus === 'UPLOADED' || document.documentStatus === 'PENDING' || document.documentStatus === 'REJECTED';
  }

  documentCanReject(document: BorrowerDocumentResponse): boolean {
    return document.documentStatus === 'UPLOADED' || document.documentStatus === 'PENDING' || document.documentStatus === 'VERIFIED';
  }

  private restoreSession(): void {
    this.bootstrapping = true;
    this.api.me().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.authSession.setUserInfo(user);
        this.bootstrapping = false;
        this.refreshAll(true);
      },
      error: () => {
        this.bootstrapping = false;
        this.clearSession('Stored session expired. Please sign in again.');
      }
    });
  }

  private clearSession(message: string): void {
    this.authSession.clear();
    this.authResponse = null;
    this.currentUser = null;
    this.reviewers = [];
    this.borrowers = [];
    this.borrowerDocuments = [];
    this.loanProducts = [];
    this.applications = [];
    this.accounts = [];
    this.rules = [];
    this.report = null;
    this.selectedBorrower = null;
    this.selectedApplication = null;
    this.selectedAccount = null;
    this.actionBusy = null;
    this.pageBusy = false;
    this.notice = { kind: 'info', text: message };
  }

  private loadBorrowerDocuments(borrowerId: number): void {
    this.api.borrowerDocuments(borrowerId).subscribe({
      next: (documents) => {
        this.borrowerDocuments = documents;
      },
      error: (error) => this.handleError(error, 'Unable to load borrower document queue')
    });
  }

  private reloadBorrowersAndDocuments(borrowerId: number, successText: string): void {
    this.api.borrowers(this.borrowerFilters()).subscribe({
      next: (borrowers) => {
        this.borrowers = borrowers.items;
        const match = borrowers.items.find((item) => item.id === borrowerId);
        if (match) {
          this.selectedBorrower = match;
          this.loadBorrowerDocuments(match.id);
        }
        this.notice = { kind: 'success', text: successText };
      },
      error: (error) => this.handleError(error, 'Unable to refresh borrower KYC data')
    });
  }

  private reloadAccounts(applicationId?: number): void {
    this.api.loanAccounts(0, this.listPageSize).subscribe({
      next: (accounts) => {
        this.accounts = accounts.items;
        if (applicationId) {
          this.api.loanAccountByApplication(applicationId).subscribe({
            next: (account) => this.selectAccount(account),
            error: (error) => this.handleError(error, 'Unable to reload the new loan account')
          });
          return;
        }

        const selectedId = this.selectedAccount?.id;
        const match = accounts.items.find((item) => item.id === selectedId) ?? accounts.items[0] ?? null;
        if (match) {
          this.selectAccount(match);
        } else {
          this.selectedAccount = null;
        }
      },
      error: (error) => this.handleError(error, 'Unable to refresh loan accounts')
    });
  }

  private reloadDashboard(): void {
    this.api.dashboard().subscribe({
      next: (summary) => {
        this.summary = summary;
      },
      error: (error) => this.handleError(error, 'Unable to refresh dashboard summary')
    });
  }

  private replaceApplication(application: LoanApplicationResponse): void {
    this.selectedApplication = application;
    this.applications = [application, ...this.applications.filter((item) => item.id !== application.id)];
    this.patchApplicationForms(application);
  }

  private patchApplicationForms(application: LoanApplicationResponse): void {
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

  private patchRepaymentForm(account: LoanAccountResponse): void {
    this.repaymentForm.patchValue({
      amount: account.monthlyInstallmentAmount,
      transactionReference: `TXN-${account.accountNumber}-${Date.now()}`,
      paymentDate: this.today()
    });
  }

  private borrowerFilters(): { businessPan?: string; businessName?: string; page: number; size: number } {
    const value = this.borrowerSearchForm.getRawValue() as Record<string, string>;
    return {
      businessPan: value['businessPan'] || undefined,
      businessName: value['businessName'] || undefined,
      page: 0,
      size: this.listPageSize
    };
  }

  private productFilters(): { name?: string; active?: boolean | null; amount?: number; maxTenureMonths?: number } {
    const value = this.loanProductSearchForm.getRawValue() as Record<string, string>;
    return {
      name: value['name'] || undefined,
      active: value['active'] === '' ? null : value['active'] === 'true' ? true : value['active'] === 'false' ? false : null,
      amount: value['amount'] ? Number(value['amount']) : undefined,
      maxTenureMonths: value['maxTenureMonths'] ? Number(value['maxTenureMonths']) : undefined
    };
  }

  private applicationFilters(): { status?: string | null; page: number; size: number } {
    const value = this.applicationSearchForm.getRawValue() as Record<string, string>;
    return {
      status: value['status'] || undefined,
      page: 0,
      size: this.listPageSize
    };
  }

  private reportFilters(): { from?: string; to?: string } {
    const value = this.reportForm.getRawValue() as Record<string, string>;
    return {
      from: value['from'] || undefined,
      to: value['to'] || undefined
    };
  }

  private createAddressGroup(addressType: BorrowerAddressRequest['addressType'] = 'REGISTERED') {
    return this.fb.group<any>({
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

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private handleError(error: unknown, fallbackText: string): void {
    const httpError = error as HttpErrorResponse;
    const apiError = httpError?.error as ApiErrorResponse | string | undefined;
    const detail = typeof apiError === 'string'
      ? apiError
      : apiError?.message || httpError?.message || fallbackText;

    if (httpError?.status === 409) {
      this.notice = {
        kind: 'warning',
        text: `Concurrent update detected (409 Conflict). ${detail} Reload the application and retry the action.`
      };
      return;
    }

    if (httpError?.status === 422) {
      this.notice = {
        kind: 'warning',
        text: `Workflow blocked by business rules (422 Unprocessable Entity). ${detail}`
      };
      return;
    }

    this.notice = { kind: 'danger', text: `${fallbackText}. ${detail}` };
  }

  private runAction<T>(
    key: string,
    action: () => Observable<T>,
    onSuccess: (value: T) => void,
    fallbackText: string
  ): void {
    this.actionBusy = key;
    action().subscribe({
      next: (value) => {
        this.actionBusy = null;
        onSuccess(value);
      },
      error: (error) => {
        this.actionBusy = null;
        this.handleError(error, fallbackText);
      }
    });
  }
}
