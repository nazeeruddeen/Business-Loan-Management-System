import { Routes } from '@angular/router';
import { FilterDataComponent } from './filter-data/filter-data.component';
import { TaskBoardComponent } from './task-board/task-board.component';
import { EmployeeDataComponent } from './employee-data/employee-data.component';
import { LoanTaskBoardComponent } from './loan/loan-task-board/loan-task-board.component';
import { OverviewComponent } from './loan/overview/overview.component';
import { ViewEmployeesComponent } from './view-employees/view-employees.component';
import { LoginComponent } from './auth/login/login.component';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';
import { UserAdminComponent } from './auth/user-admin/user-admin.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: ViewEmployeesComponent, canActivate: [authGuard] },
  { path: 'employee', component: EmployeeDataComponent, canActivate: [authGuard] },
  { path: 'taskboard', component: TaskBoardComponent, canActivate: [authGuard] },
  { path: 'filterdata', component: FilterDataComponent, canActivate: [authGuard] },
  {
    path: 'admin/users',
    component: UserAdminComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'loan',
    component: LoanTaskBoardComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      { path: 'overview', component: OverviewComponent },
      { path: 'business-product', component: OverviewComponent },
      { path: 'company-details', component: OverviewComponent },
      { path: 'company-address', component: OverviewComponent },
      { path: 'assurancedetails', component: OverviewComponent },
      { path: 'salesreport', component: OverviewComponent },
      { path: 'transactions', component: OverviewComponent },
      { path: 'txn-filters', component: OverviewComponent },
      { path: 'txn-statement', component: OverviewComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];
