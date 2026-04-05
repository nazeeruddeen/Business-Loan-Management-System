import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { EligibilityRuleResponse, LoanProductResponse } from '../business-loan.models';

@Component({
  selector: 'app-business-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Loan products and eligibility rules</h2>
        <button type="button" class="ghost" (click)="searchProducts.emit()">Reload products</button>
      </header>

      <form class="form" [formGroup]="loanProductSearchForm">
        <div class="row">
          <label>
            Product name
            <input type="text" formControlName="name">
          </label>
          <label>
            Active
            <select formControlName="active">
              <option value="">Any</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </label>
        </div>
      </form>

      <div class="split-grid">
        <form class="form" [formGroup]="loanProductForm">
          <div class="row">
            <label>
              Product code
              <input type="text" formControlName="productCode" data-testid="business-product-code">
            </label>
            <label>
              Product name
              <input type="text" formControlName="name" data-testid="business-product-name">
            </label>
          </div>
          <div class="row">
            <label>
              Min amount
              <input type="number" formControlName="minAmount" data-testid="business-product-min-amount">
            </label>
            <label>
              Max amount
              <input type="number" formControlName="maxAmount" data-testid="business-product-max-amount">
            </label>
          </div>
          <div class="row">
            <label>
              Interest rate
              <input type="number" formControlName="interestRate" step="0.1" data-testid="business-product-interest-rate">
            </label>
            <label>
              Tenure months
              <input type="number" formControlName="tenureMonths" min="1" data-testid="business-product-tenure">
            </label>
          </div>
          <label>
            Eligibility criteria
            <textarea rows="3" formControlName="eligibilityCriteria" data-testid="business-product-criteria"></textarea>
          </label>
          <button type="button" class="primary" (click)="createLoanProduct.emit()" [disabled]="actionBusy === 'createProduct'" data-testid="business-create-product">
            {{ actionBusy === 'createProduct' ? 'Creating...' : 'Create product' }}
          </button>
        </form>

        <form class="form" [formGroup]="ruleForm">
          <div class="section-subtitle">Eligibility rule engine</div>
          <div class="row">
            <label>
              Rule code
              <input type="text" formControlName="ruleCode" data-testid="business-rule-code">
            </label>
            <label>
              Rule type
              <input type="text" formControlName="ruleType" data-testid="business-rule-type">
            </label>
          </div>
          <label>
            Rule expression
            <input type="text" formControlName="ruleExpression" data-testid="business-rule-expression">
          </label>
          <div class="row">
            <label>
              Min value
              <input type="number" formControlName="minValue" data-testid="business-rule-min-value">
            </label>
            <label>
              Max value
              <input type="number" formControlName="maxValue" data-testid="business-rule-max-value">
            </label>
          </div>
          <label>
            Rule text
            <input type="text" formControlName="ruleValueText" data-testid="business-rule-text">
          </label>
          <button type="button" class="primary" (click)="createRule.emit()" [disabled]="actionBusy === 'createRule'" data-testid="business-create-rule">
            {{ actionBusy === 'createRule' ? 'Creating...' : 'Create rule' }}
          </button>
        </form>
      </div>

      <div class="list">
        <article class="list-card" *ngFor="let product of loanProducts">
          <strong>{{ product.productCode }} · {{ product.name }}</strong>
          <span>₹{{ formatMoney(product.minAmount) }} - ₹{{ formatMoney(product.maxAmount) }} · {{ product.interestRate }}%</span>
          <small>Tenure {{ product.tenureMonths }} months · {{ product.active ? 'Active' : 'Inactive' }}</small>
        </article>
      </div>

      <div class="list">
        <article class="list-card" *ngFor="let rule of rules">
          <strong>{{ rule.ruleCode }}</strong>
          <span>v{{ rule.version }} · {{ rule.ruleType }}</span>
          <small>{{ rule.ruleExpression }}</small>
        </article>
      </div>
    </article>
  `
})
export class BusinessProductsComponent {
  @Input({ required: true }) loanProductSearchForm!: FormGroup;
  @Input({ required: true }) loanProductForm!: FormGroup;
  @Input({ required: true }) ruleForm!: FormGroup;
  @Input({ required: true }) loanProducts!: LoanProductResponse[];
  @Input({ required: true }) rules!: EligibilityRuleResponse[];
  @Input() actionBusy: string | null = null;

  @Output() searchProducts = new EventEmitter<void>();
  @Output() createLoanProduct = new EventEmitter<void>();
  @Output() createRule = new EventEmitter<void>();

  formatMoney(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }
}
