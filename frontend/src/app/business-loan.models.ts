export type AddressType = 'REGISTERED' | 'OPERATIONAL' | 'CORRESPONDENCE';
export type ApplicationStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'DISBURSED';
export type LoanAccountStatus = 'ACTIVE' | 'CLOSED' | 'DEFAULTED';
export type PaymentMode = 'CASH' | 'UPI' | 'NEFT' | 'RTGS' | 'CHEQUE' | 'CARD' | 'BANK_TRANSFER';
export type EligibilityRuleType = 'MIN_VALUE' | 'MAX_VALUE' | 'RANGE' | 'TEXT_MATCH';

export interface BorrowerAddressRequest {
  addressType: AddressType;
  lineOne: string;
  lineTwo?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface CreateBorrowerRequest {
  legalBusinessName: string;
  contactPersonName: string;
  businessPan: string;
  gstin?: string;
  email: string;
  phoneNumber: string;
  industryType: string;
  annualTurnover: number | string;
  monthlyIncome: number | string;
  addresses: BorrowerAddressRequest[];
}

export interface BorrowerAddressResponse {
  id: number;
  addressType: AddressType;
  lineOne: string;
  lineTwo?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface BorrowerResponse {
  id: number;
  legalBusinessName: string;
  contactPersonName: string;
  businessPan: string;
  gstin?: string;
  email: string;
  phoneNumber: string;
  industryType: string;
  annualTurnover: number;
  monthlyIncome: number;
  createdAt: string;
  addresses: BorrowerAddressResponse[];
}

export interface CreateLoanProductRequest {
  productCode: string;
  name: string;
  minAmount: number | string;
  maxAmount: number | string;
  interestRate: number | string;
  tenureMonths: number;
  eligibilityCriteria?: string;
  active: boolean;
}

export interface LoanProductResponse {
  id: number;
  productCode: string;
  name: string;
  minAmount: number;
  maxAmount: number;
  interestRate: number;
  tenureMonths: number;
  eligibilityCriteria?: string;
  active: boolean;
  createdAt: string;
}

export interface CreateLoanApplicationRequest {
  borrowerId: number;
  loanProductId: number;
  requestedAmount: number | string;
  requestedTenureMonths: number;
  purpose: string;
}

export interface AssignReviewerRequest {
  reviewerUsername: string;
}

export interface ApplicationDecisionRequest {
  approved: boolean;
  remarks?: string;
}

export interface DisburseLoanRequest {
  disbursementReference: string;
  disbursementDate: string;
}

export interface RecordRepaymentRequest {
  amount: number | string;
  paymentMode: PaymentMode;
  transactionReference: string;
  paymentDate: string;
  notes?: string;
}

export interface CreateEligibilityRuleRequest {
  ruleCode: string;
  ruleExpression: string;
  ruleType: EligibilityRuleType;
  minValue?: number | string;
  maxValue?: number | string;
  ruleValueText?: string;
  active: boolean;
}

export interface EvaluateEligibilityRequest {
  borrowerId: number;
  loanProductId: number;
  requestedAmount: number | string;
  requestedTenureMonths: number;
}

export interface RuleEvaluationResponse {
  ruleCode: string;
  ruleExpression: string;
  passed: boolean;
  message: string;
}

export interface EligibilityEvaluationResponse {
  eligible: boolean;
  summary: string;
  ruleResults: RuleEvaluationResponse[];
}

export interface EligibilityRuleResponse {
  id: number;
  ruleCode: string;
  ruleExpression: string;
  ruleType: EligibilityRuleType;
  minValue?: number;
  maxValue?: number;
  ruleValueText?: string;
  active: boolean;
}

export interface ApplicationStatusHistoryResponse {
  fromStatus: ApplicationStatus;
  toStatus: ApplicationStatus;
  remarks?: string;
  changedBy?: string;
  changedAt: string;
}

export interface LoanApplicationResponse {
  id: number;
  borrowerId: number;
  borrowerName: string;
  loanProductId: number;
  loanProductCode: string;
  requestedAmount: number;
  requestedTenureMonths: number;
  purpose: string;
  status: ApplicationStatus;
  eligibilityPassed: boolean;
  eligibilitySummary: string;
  reviewerUsername?: string;
  submittedAt?: string;
  decisionedAt?: string;
  disbursedAt?: string;
  decisionRemarks?: string;
  history: ApplicationStatusHistoryResponse[];
}

export interface RepaymentInstallmentResponse {
  installmentNumber: number;
  dueDate: string;
  principalDue: number;
  interestDue: number;
  totalDue: number;
  principalPaid: number;
  interestPaid: number;
  remainingBalance: number;
  status: string;
}

export interface LoanRepaymentTransactionResponse {
  id: number;
  transactionReference: string;
  amount: number;
  appliedPrincipalAmount: number;
  appliedInterestAmount: number;
  paymentMode: PaymentMode;
  paymentDate: string;
  notes?: string;
  recordedBy?: string;
  recordedAt: string;
}

export interface LoanAccountResponse {
  id: number;
  applicationId: number;
  accountNumber: string;
  borrowerName: string;
  productCode: string;
  principalAmount: number;
  annualInterestRate: number;
  tenureMonths: number;
  monthlyInstallmentAmount: number;
  outstandingPrincipal: number;
  disbursementReference?: string;
  status: LoanAccountStatus;
  disbursedAt: string;
  nextDueDate?: string;
  installments: RepaymentInstallmentResponse[];
  transactions: LoanRepaymentTransactionResponse[];
}

export interface BusinessLoanDashboardResponse {
  totalLoanApplications: number;
  approvedLoanApplications: number;
  disbursedLoanAccounts: number;
  activeLoanAccounts: number;
  overdueInstallments: number;
  totalPrincipalDisbursed: number;
  totalOutstandingPrincipal: number;
  totalRepaidAmount: number;
}

export interface DisbursementReportItem {
  accountId: number;
  accountNumber: string;
  applicationId: number;
  borrowerName: string;
  productCode: string;
  principalAmount: number;
  outstandingPrincipal: number;
  status: LoanAccountStatus;
  disbursedAt: string;
  nextDueDate?: string;
}

export interface DisbursementReportResponse {
  fromDate?: string;
  toDate?: string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  disbursedCount: number;
  totalPrincipalDisbursed: number;
  totalOutstandingPrincipal: number;
  items: DisbursementReportItem[];
}

