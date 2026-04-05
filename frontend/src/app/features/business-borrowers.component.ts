import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BorrowerDocumentResponse, BorrowerDocumentStatus, BorrowerResponse } from '../business-loan.models';

@Component({
  selector: 'app-business-borrowers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Borrower onboarding and KYC</h2>
        <button type="button" class="ghost" (click)="searchBorrowers.emit()">Reload borrowers</button>
      </header>

      <div class="split-grid">
        <section>
          <form class="form" [formGroup]="borrowerSearchForm">
            <div class="row">
              <label>
                PAN
                <input type="text" formControlName="businessPan" placeholder="ABCDE1234F">
              </label>
              <label>
                Business name
                <input type="text" formControlName="businessName" placeholder="Business name">
              </label>
            </div>
          </form>

          <form class="form" [formGroup]="borrowerForm" data-testid="business-borrower-form">
            <div class="row">
              <label>
                Legal business name
                <input type="text" formControlName="legalBusinessName" data-testid="business-borrower-name">
              </label>
              <label>
                Contact person
                <input type="text" formControlName="contactPersonName">
              </label>
            </div>

            <div class="row">
              <label>
                PAN
                <input type="text" formControlName="businessPan" maxlength="10" data-testid="business-borrower-pan">
              </label>
              <label>
                GSTIN
                <input type="text" formControlName="gstin" maxlength="15">
              </label>
            </div>

            <div class="row">
              <label>
                Email
                <input type="email" formControlName="email" data-testid="business-borrower-email">
              </label>
              <label>
                Phone number
                <input type="text" formControlName="phoneNumber" data-testid="business-borrower-phone">
              </label>
            </div>

            <div class="row">
              <label>
                Industry type
                <input type="text" formControlName="industryType" data-testid="business-borrower-industry">
              </label>
              <label>
                Annual turnover
                <input type="number" formControlName="annualTurnover" min="1" step="1" data-testid="business-borrower-turnover">
              </label>
            </div>

            <div class="row">
              <label>
                Monthly income
                <input type="number" formControlName="monthlyIncome" min="1" step="1" data-testid="business-borrower-income">
              </label>
              <div class="spacer"></div>
            </div>

            <div class="section-subtitle">
              <span>Addresses</span>
              <button type="button" class="tiny" (click)="addAddress.emit()">Add address</button>
            </div>

            <div formArrayName="addresses">
              <div class="address-card" *ngFor="let address of addressArray.controls; let i = index" [formGroupName]="i">
                <div class="row">
                  <label>
                    Type
                    <select formControlName="addressType">
                      <option *ngFor="let type of addressTypes" [value]="type">{{ type }}</option>
                    </select>
                  </label>
                  <label>
                    Line one
                    <input type="text" formControlName="lineOne" data-testid="business-address-line-one">
                  </label>
                </div>

                <div class="row">
                  <label>
                    Line two
                    <input type="text" formControlName="lineTwo">
                  </label>
                  <label>
                    City
                    <input type="text" formControlName="city" data-testid="business-address-city">
                  </label>
                </div>

                <div class="row">
                  <label>
                    State
                    <input type="text" formControlName="state" data-testid="business-address-state">
                  </label>
                  <label>
                    Postal code
                    <input type="text" formControlName="postalCode" data-testid="business-address-postal-code">
                  </label>
                </div>

                <div class="row">
                  <label>
                    Country
                    <input type="text" formControlName="country" data-testid="business-address-country">
                  </label>
                  <div class="inline-actions align-end">
                    <button type="button" class="tiny danger" (click)="removeAddress.emit(i)" [disabled]="addressArray.length === 1">Remove</button>
                  </div>
                </div>
              </div>
            </div>

            <button type="button" class="primary" (click)="createBorrower.emit()" [disabled]="actionBusy === 'createBorrower'" data-testid="business-create-borrower">
              {{ actionBusy === 'createBorrower' ? 'Creating...' : 'Create borrower' }}
            </button>
          </form>

          <div class="list">
            <article
              class="list-card list-card--selectable"
              *ngFor="let borrower of borrowers"
              (click)="selectBorrower.emit(borrower)"
              [class.is-selected]="selectedBorrower?.id === borrower.id"
              [attr.data-testid]="'business-borrower-card-' + borrower.id">
              <strong>{{ borrower.legalBusinessName }}</strong>
              <span>{{ borrower.businessPan }} · {{ borrower.industryType }}</span>
              <small>{{ borrower.contactPersonName }} · {{ borrower.phoneNumber }}</small>
              <div class="chip-list">
                <span class="chip" [attr.data-kind]="kycBadgeKind(borrower)">
                  {{ borrower.kycSummary.kycComplete ? 'KYC complete' : 'KYC pending' }}
                </span>
                <span class="chip muted">{{ borrower.kycSummary.verifiedDocumentCount }}/{{ borrower.kycSummary.requiredDocumentCount }} verified</span>
              </div>
            </article>
          </div>
        </section>

        <section class="kyc-panel" *ngIf="selectedBorrower">
          <div class="selected">
            <div class="selected__header">
              <strong>{{ selectedBorrower.legalBusinessName }}</strong>
              <span class="chip" [attr.data-kind]="kycBadgeKind(selectedBorrower)">
                {{ selectedBorrower.kycSummary.kycComplete ? 'KYC complete' : 'KYC pending' }}
              </span>
            </div>
            <p>{{ selectedBorrower.contactPersonName }} · {{ selectedBorrower.email }}</p>
            <small *ngIf="selectedBorrower.kycSummary.missingRequiredDocuments.length">
              Missing: {{ selectedBorrower.kycSummary.missingRequiredDocuments.join(', ') }}
            </small>
          </div>

          <div class="selected">
            <div class="selected__header">
              <strong>{{ selectedBorrower.kycSummary.kycComplete ? 'KYC ready for workflow' : 'KYC blocks workflow' }}</strong>
              <span class="chip" [attr.data-kind]="selectedBorrower.kycSummary.kycComplete ? 'success' : 'warning'">
                {{ selectedBorrower.kycSummary.kycComplete ? 'Ready' : 'Blocked' }}
              </span>
            </div>
            <p *ngIf="selectedBorrower.kycSummary.kycComplete">
              All required documents are verified for this borrower.
            </p>
            <p *ngIf="!selectedBorrower.kycSummary.kycComplete">
              Missing verified documents: {{ selectedBorrower.kycSummary.missingRequiredDocuments.join(', ') }}
            </p>
          </div>

          <form class="form" [formGroup]="documentForm" data-testid="business-document-form">
            <div class="section-subtitle">Document intake</div>
            <div class="row">
              <label>
                Document type
                <select formControlName="documentType" data-testid="business-document-type">
                  <option *ngFor="let type of documentTypes" [value]="type">{{ formatLabel(type) }}</option>
                </select>
              </label>
              <label>
                File name
                <input type="text" formControlName="fileName" placeholder="pan-card.pdf" data-testid="business-document-file-name">
              </label>
            </div>

            <div class="row">
              <label>
                File reference
                <input type="text" formControlName="fileReference" placeholder="s3://kyc/bizloan/pan-card.pdf" data-testid="business-document-file-reference">
              </label>
              <label>
                Remarks
                <input type="text" formControlName="remarks" placeholder="Uploaded by ops team">
              </label>
            </div>

            <button type="button" class="primary" (click)="createBorrowerDocument.emit()" [disabled]="actionBusy === 'createDocument'" data-testid="business-add-document">
              {{ actionBusy === 'createDocument' ? 'Saving...' : 'Add document metadata' }}
            </button>
          </form>

          <form class="form" [formGroup]="documentReviewForm">
            <div class="section-subtitle">Document review remarks</div>
            <label>
              Review remarks
              <input type="text" formControlName="remarks" placeholder="Verified against submitted KYC pack">
            </label>
          </form>

          <div class="list">
            <article class="list-card" *ngFor="let document of borrowerDocuments" [attr.data-testid]="'business-document-card-' + document.documentType">
              <strong>{{ formatLabel(document.documentType) }}</strong>
              <span>{{ document.fileName }} · {{ document.documentStatus }}</span>
              <small>{{ document.fileReference }}</small>
              <div class="chip-list">
                <span class="chip muted" *ngIf="document.requiredDocument">Required</span>
                <span class="chip muted" *ngIf="document.reviewedBy">Reviewed by {{ document.reviewedBy }}</span>
              </div>
              <div class="inline-actions">
                <button type="button" class="ghost tiny" (click)="reviewDocumentAction.emit({ document, status: 'VERIFIED' })" [disabled]="!documentCanVerify(document) || actionBusy === 'reviewDocument-' + document.id" [attr.data-testid]="'business-verify-document-' + document.documentType">
                  Verify
                </button>
                <button type="button" class="danger tiny" (click)="reviewDocumentAction.emit({ document, status: 'REJECTED' })" [disabled]="!documentCanReject(document) || actionBusy === 'reviewDocument-' + document.id">
                  Reject
                </button>
              </div>
            </article>
          </div>
        </section>
      </div>
    </article>
  `
})
export class BusinessBorrowersComponent {
  @Input({ required: true }) borrowerSearchForm!: FormGroup;
  @Input({ required: true }) borrowerForm!: FormGroup;
  @Input({ required: true }) addressArray!: FormArray;
  @Input({ required: true }) addressTypes!: string[];
  @Input({ required: true }) borrowers!: BorrowerResponse[];
  @Input() selectedBorrower: BorrowerResponse | null = null;
  @Input({ required: true }) borrowerDocuments!: BorrowerDocumentResponse[];
  @Input({ required: true }) documentForm!: FormGroup;
  @Input({ required: true }) documentReviewForm!: FormGroup;
  @Input({ required: true }) documentTypes!: string[];
  @Input() actionBusy: string | null = null;

  @Output() searchBorrowers = new EventEmitter<void>();
  @Output() addAddress = new EventEmitter<void>();
  @Output() removeAddress = new EventEmitter<number>();
  @Output() createBorrower = new EventEmitter<void>();
  @Output() selectBorrower = new EventEmitter<BorrowerResponse>();
  @Output() createBorrowerDocument = new EventEmitter<void>();
  @Output() reviewDocumentAction = new EventEmitter<{ document: BorrowerDocumentResponse; status: BorrowerDocumentStatus }>();

  formatLabel(value: string): string {
    return value.replace(/_/g, ' ');
  }

  kycBadgeKind(borrower: BorrowerResponse | null): 'success' | 'warning' | 'info' {
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
}























