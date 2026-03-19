import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { environment } from '../environments/environment';
import {
  ApplicationDecisionRequest,
  AssignReviewerRequest,
  BorrowerResponse,
  BusinessLoanDashboardResponse,
  CreateBorrowerRequest,
  CreateEligibilityRuleRequest,
  CreateLoanApplicationRequest,
  CreateLoanProductRequest,
  DisburseLoanRequest,
  DisbursementReportResponse,
  EligibilityEvaluationResponse,
  EligibilityRuleResponse,
  EvaluateEligibilityRequest,
  LoanAccountResponse,
  LoanApplicationResponse,
  LoanProductResponse,
  RecordRepaymentRequest
} from './business-loan.models';

type QueryValue = string | number | boolean | null | undefined;

@Injectable({ providedIn: 'root' })
export class BusinessLoanApiService {
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

  constructor(private readonly http: HttpClient) {}

  dashboard(): Observable<BusinessLoanDashboardResponse> {
    return this.http.get<BusinessLoanDashboardResponse>(this.url('/loan-accounts/dashboard')).pipe(
      catchError(() => of(this.mockDashboard))
    );
  }

  borrowers(filters: { businessPan?: string; businessName?: string } = {}): Observable<BorrowerResponse[]> {
    return this.http.get<BorrowerResponse[]>(this.url('/borrowers'), { params: this.params(filters) }).pipe(
      catchError(() => of(this.mockBorrowers))
    );
  }

  createBorrower(payload: CreateBorrowerRequest): Observable<BorrowerResponse> {
    return this.http.post<BorrowerResponse>(this.url('/borrowers'), payload);
  }

  loanProducts(filters: { name?: string; active?: boolean | null; amount?: number; maxTenureMonths?: number } = {}): Observable<LoanProductResponse[]> {
    return this.http.get<LoanProductResponse[]>(this.url('/loan-products'), { params: this.params(filters) }).pipe(
      catchError(() => of(this.mockLoanProducts))
    );
  }

  createLoanProduct(payload: CreateLoanProductRequest): Observable<LoanProductResponse> {
    return this.http.post<LoanProductResponse>(this.url('/loan-products'), payload);
  }

  eligibilityRules(): Observable<EligibilityRuleResponse[]> {
    return this.http.get<EligibilityRuleResponse[]>(this.url('/eligibility-rules')).pipe(
      catchError(() => of(this.mockRules))
    );
  }

  createEligibilityRule(payload: CreateEligibilityRuleRequest): Observable<EligibilityRuleResponse> {
    return this.http.post<EligibilityRuleResponse>(this.url('/eligibility-rules'), payload);
  }

  evaluateEligibility(payload: EvaluateEligibilityRequest): Observable<EligibilityEvaluationResponse> {
    return this.http.post<EligibilityEvaluationResponse>(this.url('/eligibility/evaluate'), payload).pipe(
      catchError(() => of({
        eligible: true,
        summary: 'Mock evaluation: borrower passes policy checks.',
        ruleResults: [
          { ruleCode: 'INCOME_MIN', ruleExpression: 'Monthly income above threshold', passed: true, message: 'Pass' },
          { ruleCode: 'TENURE_MAX', ruleExpression: 'Requested tenure within policy', passed: true, message: 'Pass' }
        ]
      }))
    );
  }

  applications(filters: { status?: string | null } = {}): Observable<LoanApplicationResponse[]> {
    return this.http.get<LoanApplicationResponse[]>(this.url('/loan-applications'), { params: this.params(filters) }).pipe(
      catchError(() => of(this.mockApplications))
    );
  }

  createLoanApplication(payload: CreateLoanApplicationRequest): Observable<LoanApplicationResponse> {
    return this.http.post<LoanApplicationResponse>(this.url('/loan-applications'), payload);
  }

  submitApplication(applicationId: number): Observable<LoanApplicationResponse> {
    return this.http.post<LoanApplicationResponse>(this.url(`/loan-applications/${applicationId}/submit`), {});
  }

  assignReviewer(applicationId: number, payload: AssignReviewerRequest): Observable<LoanApplicationResponse> {
    return this.http.post<LoanApplicationResponse>(this.url(`/loan-applications/${applicationId}/assign-reviewer`), payload);
  }

  decideApplication(applicationId: number, payload: ApplicationDecisionRequest): Observable<LoanApplicationResponse> {
    return this.http.post<LoanApplicationResponse>(this.url(`/loan-applications/${applicationId}/decision`), payload);
  }

  disburseApplication(applicationId: number, payload: DisburseLoanRequest): Observable<LoanApplicationResponse> {
    return this.http.post<LoanApplicationResponse>(this.url(`/loan-applications/${applicationId}/disburse`), payload);
  }

  loanAccountByApplication(applicationId: number): Observable<LoanAccountResponse> {
    return this.http.get<LoanAccountResponse>(this.url(`/loan-accounts/application/${applicationId}`)).pipe(
      catchError(() => of(this.mockLoanAccounts[0]))
    );
  }

  loanAccountByAccountNumber(accountNumber: string): Observable<LoanAccountResponse> {
    return this.http.get<LoanAccountResponse>(this.url(`/loan-accounts/${accountNumber}`)).pipe(
      catchError(() => of(this.mockLoanAccounts[0]))
    );
  }

  loanAccounts(): Observable<LoanAccountResponse[]> {
    return this.http.get<LoanAccountResponse[]>(this.url('/loan-accounts')).pipe(
      catchError(() => of(this.mockLoanAccounts))
    );
  }

  recordRepayment(accountId: number, payload: RecordRepaymentRequest): Observable<LoanAccountResponse> {
    return this.http.post<LoanAccountResponse>(this.url(`/loan-accounts/${accountId}/repayments`), payload);
  }

  report(filters: { from?: string; to?: string } = {}, page = 0, size = 8): Observable<DisbursementReportResponse> {
    const params = this.params({ ...filters, page, size });
    return this.http.get<DisbursementReportResponse>(this.url('/reports/disbursements'), { params }).pipe(
      catchError(() => of(this.mockReport))
    );
  }

  exportDisbursements(filters: { from?: string; to?: string } = {}): Observable<string> {
    return this.http.get(this.url('/reports/disbursements/export'), {
      params: this.params(filters),
      responseType: 'text'
    }).pipe(
      catchError(() => of(this.mockDisbursementCsv))
    );
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`;
  }

  private params(filters: Record<string, QueryValue>): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value === null || value === undefined || value === '') {
        continue;
      }
      params = params.set(key, String(value));
    }
    return params;
  }

  private readonly mockBorrowers: BorrowerResponse[] = [
    {
      id: 1,
      legalBusinessName: 'Apex Traders Pvt Ltd',
      contactPersonName: 'Naveen Rao',
      businessPan: 'ABCDE1234F',
      gstin: '29ABCDE1234F1Z5',
      email: 'accounts@apextraders.example',
      phoneNumber: '9876543210',
      industryType: 'Wholesale Trading',
      annualTurnover: 25000000,
      monthlyIncome: 1200000,
      createdAt: new Date().toISOString(),
      addresses: [
        { id: 1, addressType: 'REGISTERED', lineOne: '12 Industrial Park', lineTwo: 'Phase 2', city: 'Hyderabad', state: 'Telangana', postalCode: '500081', country: 'India' }
      ]
    }
  ];

  private readonly mockLoanProducts: LoanProductResponse[] = [
    {
      id: 1,
      productCode: 'BL-TERM-36',
      name: 'Business Term Loan',
      minAmount: 500000,
      maxAmount: 10000000,
      interestRate: 13.5,
      tenureMonths: 36,
      eligibilityCriteria: 'Stable turnover and repayment capacity',
      active: true,
      createdAt: new Date().toISOString()
    }
  ];

  private readonly mockRules: EligibilityRuleResponse[] = [
    { id: 1, ruleCode: 'INCOME_MIN', ruleExpression: 'Monthly income >= 100000', ruleType: 'MIN_VALUE', minValue: 100000, active: true }
  ];

  private readonly mockApplications: LoanApplicationResponse[] = [
    {
      id: 101,
      borrowerId: 1,
      borrowerName: 'Apex Traders Pvt Ltd',
      loanProductId: 1,
      loanProductCode: 'BL-TERM-36',
      requestedAmount: 2500000,
      requestedTenureMonths: 36,
      purpose: 'Working capital expansion',
      status: 'APPROVED',
      eligibilityPassed: true,
      eligibilitySummary: 'Eligible',
      reviewerUsername: 'reviewer01',
      submittedAt: new Date().toISOString(),
      decisionedAt: new Date().toISOString(),
      disbursedAt: new Date().toISOString(),
      decisionRemarks: 'Meets policy thresholds',
      history: []
    }
  ];

  private readonly mockLoanAccounts: LoanAccountResponse[] = [
    {
      id: 5001,
      applicationId: 101,
      accountNumber: 'BLA-5001',
      borrowerName: 'Apex Traders Pvt Ltd',
      productCode: 'BL-TERM-36',
      principalAmount: 2500000,
      annualInterestRate: 13.5,
      tenureMonths: 36,
      monthlyInstallmentAmount: 84321,
      outstandingPrincipal: 2140000,
      disbursementReference: 'DISB-2026-001',
      status: 'ACTIVE',
      disbursedAt: new Date().toISOString(),
      nextDueDate: new Date().toISOString().slice(0, 10),
      installments: [],
      transactions: []
    }
  ];

  private readonly mockDashboard: BusinessLoanDashboardResponse = {
    totalLoanApplications: 14,
    approvedLoanApplications: 9,
    disbursedLoanAccounts: 6,
    activeLoanAccounts: 5,
    overdueInstallments: 2,
    totalPrincipalDisbursed: 18500000,
    totalOutstandingPrincipal: 12400000,
    totalRepaidAmount: 6100000
  };

  private readonly mockReport = {
    fromDate: undefined,
    toDate: undefined,
    page: 0,
    size: 8,
    totalElements: 1,
    totalPages: 1,
    disbursedCount: 1,
    totalPrincipalDisbursed: 2500000,
    totalOutstandingPrincipal: 2140000,
    items: [
      {
        accountId: 5001,
        accountNumber: 'BLA-5001',
        applicationId: 101,
        borrowerName: 'Apex Traders Pvt Ltd',
        productCode: 'BL-TERM-36',
        principalAmount: 2500000,
        outstandingPrincipal: 2140000,
        status: 'ACTIVE',
        disbursedAt: new Date().toISOString(),
        nextDueDate: new Date().toISOString().slice(0, 10)
      }
    ]
  } as DisbursementReportResponse;

  private readonly mockDisbursementCsv = [
    'accountNumber,applicationId,borrowerName,productCode,principalAmount,outstandingPrincipal,status',
    'BLA-5001,101,Apex Traders Pvt Ltd,BL-TERM-36,2500000,2140000,ACTIVE'
  ].join('\n');
}

