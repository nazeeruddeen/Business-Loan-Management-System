import { HttpResponse } from '@angular/common/http';
import { Component, OnDestroy } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { EmployeeDataService } from '../../employee-data.service';
import { ThemeService } from '../../theme.service';
import { SharedModule } from '../../shared/shared.module';
import { LoanApplicationComponent } from '../loan-application/loan-application.component';
import { OverviewComponent } from '../overview/overview.component';
import { AuthorizationService } from '../../auth/authorization.service';


@Component({
  selector: 'app-loan-task-board',
  standalone: true,
  imports: [SharedModule, OverviewComponent],
  templateUrl: './loan-task-board.component.html',
  styleUrl: './loan-task-board.component.css',
})
export class LoanTaskBoardComponent implements OnDestroy {
  displayedColumns: string[] | undefined;
  loanTaskBoardData: any[] = [];
  showOverviewData = false;
  id: any;
  isLoading = false;
  canApplyLoan = false;
  private routeSub?: Subscription;

  constructor(
    private employeeDataService: EmployeeDataService,
    private dialog: MatDialog,
    private router: Router,
    private route: ActivatedRoute,
    private themeService: ThemeService,
    private authorizationService: AuthorizationService
  ) {}

  ngOnInit() {
    this.displayedColumns = ['appId', 'customerName', 'mailId', 'mobile', 'city'];
    this.canApplyLoan = this.authorizationService.canApplyLoan();
    this.loadLoanData();
    this.updateViewFromUrl();
    this.routeSub = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(() => this.updateViewFromUrl());
  }

  private updateViewFromUrl() {
    const url = this.router.url;
    const path = url.split('?')[0];
    const lastSegment = path.split('/').pop() || '';
    const isOverviewPath = this.OVERVIEW_PATHS.includes(lastSegment);
    const id = this.route.snapshot.queryParams['id'] || new URLSearchParams(window.location.search).get('id');
    this.showOverviewData = isOverviewPath && !!id;
    if (this.showOverviewData && id) {
      this.id = id;
    }
  }

  openDialog(data?: any) {
    if (!this.canApplyLoan) {
      return;
    }

    const dialogConfig = new MatDialogConfig();
    dialogConfig.disableClose = false;
    dialogConfig.autoFocus = true;
    if (data) {
      dialogConfig.data = { data };
    } else {
      dialogConfig.data = { id: '' };
    }
    if (this.themeService.isDarkTheme.value) {
      dialogConfig.panelClass = ['app-dark-dialog'];
    }
    const dialogRef = this.dialog.open(LoanApplicationComponent, dialogConfig);
    dialogRef.afterClosed().subscribe((result) => {
      if (result?.success) {
        this.loadLoanData();
      }
    });
  }

  loadLoanData() {
    this.isLoading = true;
    this.employeeDataService.getLoanTaskboardData().subscribe({
      next: (response: HttpResponse<any>) => {
        this.loanTaskBoardData = response.body ?? [];
      },
      error: () => {
        this.loanTaskBoardData = [];
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }
  private readonly OVERVIEW_PATHS = ['overview', 'business-product', 'company-details', 'company-address', 'assurancedetails', 'salesreport', 'transactions', 'txn-filters', 'txn-statement'];

  showOverview(id: any) {
    this.showOverviewData = true;
    this.id = id;
    this.router.navigate(['/loan/overview'], { queryParams: { id: this.id }, replaceUrl: false });
  }
  ngOnDestroy() {
    this.routeSub?.unsubscribe();
  }
}
