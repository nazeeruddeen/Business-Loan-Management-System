import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';

type AppRole = 'ADMIN' | 'LOAN_OFFICER' | 'ANALYST' | 'VIEWER';

@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {
  constructor(private authService: AuthService) {}

  getRole(): AppRole | null {
    const role = this.authService.getRole()?.trim().toUpperCase();
    if (!role) {
      return null;
    }

    if (role === 'ADMIN' || role === 'LOAN_OFFICER' || role === 'ANALYST' || role === 'VIEWER') {
      return role;
    }

    return null;
  }

  hasAnyRole(...roles: AppRole[]): boolean {
    const role = this.getRole();
    return !!role && roles.includes(role);
  }

  canApplyLoan(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER');
  }

  canViewAssurance(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER');
  }

  canEditAssurance(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER');
  }

  canViewSalesReport(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER', 'ANALYST');
  }

  canEditSalesReport(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER');
  }

  canViewTransactions(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER', 'ANALYST');
  }

  canEditTransactions(): boolean {
    return this.hasAnyRole('ADMIN', 'LOAN_OFFICER');
  }

  getVisibleLoanTabs(): string[] {
    const baseTabs = ['overview', 'business-product', 'company-details', 'company-address'];

    if (this.canViewAssurance()) {
      baseTabs.push('assurancedetails');
    }

    if (this.canViewSalesReport()) {
      baseTabs.push('salesreport');
    }

    if (this.canViewTransactions()) {
      baseTabs.push('transactions', 'txn-filters', 'txn-statement');
    }

    return baseTabs;
  }
}
