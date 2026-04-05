import { FormBuilder, Validators } from '@angular/forms';
import {
  BorrowerAddressRequest,
  PaymentMode
} from './business-loan.models';

export function createBusinessAddressGroup(fb: FormBuilder, addressType: BorrowerAddressRequest['addressType'] = 'REGISTERED') {
  return fb.group<any>({
    addressType: [addressType, Validators.required],
    lineOne: ['', [Validators.required, Validators.maxLength(160)]],
    lineTwo: [''],
    city: ['', [Validators.required, Validators.maxLength(80)]],
    state: ['', [Validators.required, Validators.maxLength(80)]],
    postalCode: ['', [Validators.required, Validators.maxLength(15)]],
    country: ['', [Validators.required, Validators.maxLength(80)]]
  });
}

export function buildBusinessAuthForm(fb: FormBuilder) {
  return fb.group<any>({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });
}

export function buildBusinessBorrowerSearchForm(fb: FormBuilder) {
  return fb.group<any>({
    businessPan: [''],
    businessName: ['']
  });
}

export function buildBusinessBorrowerForm(fb: FormBuilder) {
  return fb.group<any>({
    legalBusinessName: ['', [Validators.required, Validators.maxLength(150)]],
    contactPersonName: ['', [Validators.required, Validators.maxLength(120)]],
    businessPan: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/)]],
    gstin: [''],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    industryType: ['', [Validators.required, Validators.maxLength(80)]],
    annualTurnover: [null, [Validators.required, Validators.min(1)]],
    monthlyIncome: [null, [Validators.required, Validators.min(1)]],
    addresses: fb.array([createBusinessAddressGroup(fb, 'REGISTERED')])
  });
}

export function buildBusinessDocumentForm(fb: FormBuilder) {
  return fb.group<any>({
    documentType: ['PAN_CARD', [Validators.required]],
    fileName: ['', [Validators.required, Validators.maxLength(180)]],
    fileReference: ['', [Validators.required, Validators.maxLength(255)]],
    remarks: ['']
  });
}

export function buildBusinessDocumentReviewForm(fb: FormBuilder) {
  return fb.group<any>({
    remarks: ['']
  });
}

export function buildBusinessLoanProductSearchForm(fb: FormBuilder) {
  return fb.group<any>({
    name: [''],
    active: [''],
    amount: [''],
    maxTenureMonths: ['']
  });
}

export function buildBusinessLoanProductForm(fb: FormBuilder) {
  return fb.group<any>({
    productCode: ['', [Validators.required, Validators.maxLength(40)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    minAmount: [null, [Validators.required, Validators.min(1)]],
    maxAmount: [null, [Validators.required, Validators.min(1)]],
    interestRate: [null, [Validators.required, Validators.min(0.1)]],
    tenureMonths: [36, [Validators.required, Validators.min(1)]],
    eligibilityCriteria: [''],
    active: [true]
  });
}

export function buildBusinessApplicationSearchForm(fb: FormBuilder) {
  return fb.group<any>({
    status: ['']
  });
}

export function buildBusinessApplicationForm(fb: FormBuilder) {
  return fb.group<any>({
    borrowerId: [null, [Validators.required]],
    loanProductId: [null, [Validators.required]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]],
    purpose: ['', [Validators.required, Validators.maxLength(200)]]
  });
}

export function buildBusinessReviewerForm(fb: FormBuilder) {
  return fb.group<any>({
    reviewerUserId: [null, [Validators.required]]
  });
}

export function buildBusinessDecisionForm(fb: FormBuilder) {
  return fb.group<any>({
    decisionStatus: ['APPROVED', [Validators.required]],
    remarks: ['', [Validators.required, Validators.maxLength(250)]]
  });
}

export function buildBusinessDisbursementForm(fb: FormBuilder, today: string) {
  return fb.group<any>({
    disbursementReference: ['', [Validators.required]],
    disbursementDate: [today, [Validators.required]]
  });
}

export function buildBusinessEligibilityForm(fb: FormBuilder) {
  return fb.group<any>({
    borrowerId: [null, [Validators.required]],
    loanProductId: [null, [Validators.required]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]]
  });
}

export function buildBusinessRuleForm(fb: FormBuilder) {
  return fb.group<any>({
    ruleCode: ['INCOME_MIN', [Validators.required, Validators.maxLength(40)]],
    ruleExpression: ['Monthly income should meet policy threshold', [Validators.required, Validators.maxLength(250)]],
    ruleType: ['MIN_VALUE', [Validators.required]],
    minValue: [100000],
    maxValue: [null],
    ruleValueText: [''],
    active: [true]
  });
}

export function buildBusinessRepaymentForm(fb: FormBuilder, paymentMode: PaymentMode, today: string) {
  return fb.group<any>({
    amount: [null, [Validators.required, Validators.min(1)]],
    paymentMode: [paymentMode, [Validators.required]],
    transactionReference: ['', [Validators.required]],
    paymentDate: [today, [Validators.required]],
    notes: ['']
  });
}

export function buildBusinessReportForm(fb: FormBuilder) {
  return fb.group<any>({
    from: [''],
    to: [''],
    size: [8]
  });
}
