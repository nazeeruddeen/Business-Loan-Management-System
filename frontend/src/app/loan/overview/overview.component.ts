import { HttpResponse } from '@angular/common/http';
import { Component, Input, OnDestroy } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Location } from '@angular/common';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { EmployeeDataService } from '../../employee-data.service';
import { SharedModule } from '../../shared/shared.module';
import { TransacationStatementComponent } from '../transacation-statement/transacation-statement.component';
import { TransactionfiltersComponent } from '../transactionfilters/transactionfilters.component';
import { TrnsactionsComponent } from '../trnsactions/trnsactions.component';
import { SalesreportComponent } from '../salesreport/salesreport.component';
import { AssurancedetailsComponent } from '../assurancedetails/assurancedetails.component';
import { CompanyDetailsComponent } from '../company-details/company-details.component';
import { BusinessProductComponent } from '../business-product/business-product.component';
import { CompanyAddressComponent } from '../company-address/company-address.component';
import { AuthorizationService } from '../../auth/authorization.service';

const TAB_KEYS = ['overview', 'business-product', 'company-details', 'company-address', 'assurancedetails', 'salesreport', 'transactions', 'txn-filters', 'txn-statement'];

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [SharedModule, CompanyAddressComponent, TransacationStatementComponent, TransactionfiltersComponent, TrnsactionsComponent, SalesreportComponent, AssurancedetailsComponent, CompanyDetailsComponent, BusinessProductComponent],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css'
})
export class OverviewComponent implements OnDestroy {
  @Input() data: any;
  showOverviewData = true;
  loanForm: any;
  createNew = true;
  id: string | null = '';
  isLoading = false;
  loadError: string | null = null;
  selectedTabIndex = 0;
  visibleTabKeys: string[] = [];
  private querySub?: Subscription;

  constructor(
    private employeeDataService: EmployeeDataService,
    private formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private location: Location,
    private authorizationService: AuthorizationService
  ) {}

  ngOnInit() {
    this.id = this.route.snapshot.queryParams['id'] || this.route.parent?.snapshot?.queryParams['id'] || new URLSearchParams(window.location.search).get('id');
    this.visibleTabKeys = this.authorizationService.getVisibleLoanTabs();
    if (this.visibleTabKeys.length === 0) {
      this.visibleTabKeys = ['overview'];
    }

    this.syncTabFromUrl();
    this.querySub = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(() => this.syncTabFromUrl());
    this.createFromGroup();
    if (this.id) {
      this.isLoading = true;
      this.loadError = null;
      this.employeeDataService.getOverviewDetails(this.id).subscribe({
        next: (response) => {
          this.data = response.body;
        },
        error: (err) => {
          this.loadError = err?.error?.message || 'Failed to load overview details';
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    } else {
      this.loadError = 'Application ID not found in URL';
    }
  }

  hasTab(tabKey: string): boolean {
    return this.visibleTabKeys.includes(tabKey);
  }

  private syncTabFromUrl() {
    const queryTab = this.route.snapshot.queryParams['tab'] || this.route.parent?.snapshot?.queryParams['tab'];
    if (queryTab) {
      if (!this.hasTab(queryTab)) {
        this.redirectToFirstAllowedTab();
        return;
      }

      const queryIndex = this.visibleTabKeys.indexOf(queryTab);
      if (queryIndex >= 0 && queryIndex !== this.selectedTabIndex) {
        this.selectedTabIndex = queryIndex;
      }
      return;
    }

    const path = this.router.url.split('?')[0];
    const seg = path.split('/').pop() || '';
    const idx = TAB_KEYS.indexOf(seg);

    if (idx >= 0 && this.hasTab(seg)) {
      const visibleIdx = this.visibleTabKeys.indexOf(seg);
      if (visibleIdx >= 0 && visibleIdx !== this.selectedTabIndex) {
        this.selectedTabIndex = visibleIdx;
      }
    } else {
      const overviewIndex = this.visibleTabKeys.indexOf('overview');
      this.selectedTabIndex = overviewIndex >= 0 ? overviewIndex : 0;
    }
  }

  private redirectToFirstAllowedTab() {
    const firstAllowed = this.visibleTabKeys[0] || 'overview';
    const queryParams: { [key: string]: string } = { tab: firstAllowed };
    if (this.id) {
      queryParams['id'] = this.id;
    }

    this.selectedTabIndex = 0;
    this.router.navigate(['/loan/overview'], {
      queryParams,
      replaceUrl: true
    });
  }

  onTabChange(index: number) {
    if (index < 0 || index >= this.visibleTabKeys.length) {
      return;
    }

    this.selectedTabIndex = index;
    const tabKey = this.visibleTabKeys[index] || this.visibleTabKeys[0] || 'overview';
    const queryParams: { [key: string]: string } = { tab: tabKey };
    if (this.id) {
      queryParams['id'] = this.id;
    }

    this.router.navigate(['/loan/overview'], {
      queryParams,
      replaceUrl: true
    });
  }

  onSubmit() {
    this.employeeDataService.saveLoanApplication(this.loanForm.value).subscribe((response: HttpResponse<any>) => {
    });

  }
  hideOverview(){
   this.showOverviewData = false;
  }
  showLoanData() {
    this.location.back();
  }


  createFromGroup() {
    this.loanForm = this.formBuilder.group({
      fname: [''],
      lname: [''],
      mailId: [''],
      mobile: [''],
      city: ['']
    });
  }
  ngOnDestroy() {
    this.querySub?.unsubscribe();
  }
}
