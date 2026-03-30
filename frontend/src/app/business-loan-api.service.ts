import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import {
  ApplicationDecisionRequest,
  AssignReviewerRequest,
  AuthResponse,
  BorrowerDocumentResponse,
  BorrowerResponse,
  BusinessLoanDashboardResponse,
  CreateBorrowerDocumentRequest,
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
  LoanRepaymentTransactionResponse,
  LoginRequest,
  RecordRepaymentRequest,
  UpdateBorrowerDocumentStatusRequest,
  UserInfoResponse,
  UserSummaryResponse
} from './business-loan.models';

type QueryValue = string | number | boolean | null | undefined;

@Injectable({ providedIn: 'root' })
export class BusinessLoanApiService {
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');
  private readonly serverBaseUrl = this.baseUrl.replace(/\/api\/v1$/, '');

  constructor(private readonly http: HttpClient) {}

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.serverBaseUrl}/auth/login`, payload);
  }

  me(): Observable<UserInfoResponse> {
    return this.http.get<UserInfoResponse>(`${this.serverBaseUrl}/auth/me`);
  }

  logout(refreshToken: string): Observable<void> {
    return this.http.post<void>(`${this.serverBaseUrl}/auth/logout`, { refreshToken });
  }

  users(): Observable<UserSummaryResponse[]> {
    return this.http.get<UserSummaryResponse[]>(`${this.serverBaseUrl}/auth/users`);
  }

  dashboard(): Observable<BusinessLoanDashboardResponse> {
    return this.http.get<BusinessLoanDashboardResponse>(this.url('/loan-accounts/dashboard'));
  }

  borrowers(filters: { businessPan?: string; businessName?: string } = {}): Observable<BorrowerResponse[]> {
    return this.http.get<BorrowerResponse[]>(this.url('/borrowers'), { params: this.params(filters) });
  }

  createBorrower(payload: CreateBorrowerRequest): Observable<BorrowerResponse> {
    return this.http.post<BorrowerResponse>(this.url('/borrowers'), payload);
  }

  borrowerDocuments(borrowerId: number): Observable<BorrowerDocumentResponse[]> {
    return this.http.get<BorrowerDocumentResponse[]>(this.url(`/borrowers/${borrowerId}/documents`));
  }

  createBorrowerDocument(borrowerId: number, payload: CreateBorrowerDocumentRequest): Observable<BorrowerDocumentResponse> {
    return this.http.post<BorrowerDocumentResponse>(this.url(`/borrowers/${borrowerId}/documents`), payload);
  }

  updateBorrowerDocumentStatus(
    borrowerId: number,
    documentId: number,
    payload: UpdateBorrowerDocumentStatusRequest
  ): Observable<BorrowerDocumentResponse> {
    return this.http.patch<BorrowerDocumentResponse>(this.url(`/borrowers/${borrowerId}/documents/${documentId}/status`), payload);
  }

  loanProducts(filters: { name?: string; active?: boolean | null; amount?: number; maxTenureMonths?: number } = {}): Observable<LoanProductResponse[]> {
    return this.http.get<LoanProductResponse[]>(this.url('/loan-products'), { params: this.params(filters) });
  }

  createLoanProduct(payload: CreateLoanProductRequest): Observable<LoanProductResponse> {
    return this.http.post<LoanProductResponse>(this.url('/loan-products'), payload);
  }

  eligibilityRules(): Observable<EligibilityRuleResponse[]> {
    return this.http.get<EligibilityRuleResponse[]>(this.url('/eligibility-rules'));
  }

  createEligibilityRule(payload: CreateEligibilityRuleRequest): Observable<EligibilityRuleResponse> {
    return this.http.post<EligibilityRuleResponse>(this.url('/eligibility-rules'), payload);
  }

  evaluateEligibility(payload: EvaluateEligibilityRequest): Observable<EligibilityEvaluationResponse> {
    return this.http.post<EligibilityEvaluationResponse>(this.url('/eligibility/evaluate'), payload);
  }

  applications(filters: { status?: string | null } = {}): Observable<LoanApplicationResponse[]> {
    return this.http.get<LoanApplicationResponse[]>(this.url('/loan-applications'), { params: this.params(filters) });
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
    return this.http.get<LoanAccountResponse>(this.url(`/loan-accounts/application/${applicationId}`));
  }

  loanAccountByAccountNumber(accountNumber: string): Observable<LoanAccountResponse> {
    return this.http.get<LoanAccountResponse>(this.url(`/loan-accounts/${accountNumber}`));
  }

  loanAccounts(): Observable<LoanAccountResponse[]> {
    return this.http.get<LoanAccountResponse[]>(this.url('/loan-accounts'));
  }

  recordRepayment(accountId: number, payload: RecordRepaymentRequest): Observable<LoanRepaymentTransactionResponse> {
    return this.http.post<LoanRepaymentTransactionResponse>(this.url(`/loan-accounts/${accountId}/repayments`), payload);
  }

  report(filters: { from?: string; to?: string } = {}, page = 0, size = 8): Observable<DisbursementReportResponse> {
    const params = this.params({ ...filters, page, size });
    return this.http.get<DisbursementReportResponse>(this.url('/reports/disbursements'), { params });
  }

  exportDisbursements(filters: { from?: string; to?: string } = {}): Observable<Blob> {
    return this.http.get(this.url('/reports/disbursements/export'), {
      params: this.params(filters),
      responseType: 'blob'
    });
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
}
