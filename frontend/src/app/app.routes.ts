import { Routes } from '@angular/router';
import { BusinessLoanWorkspaceComponent } from './workspace.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  { path: 'overview', component: BusinessLoanWorkspaceComponent, data: { tab: 'dashboard' } },
  { path: 'borrowers', component: BusinessLoanWorkspaceComponent, data: { tab: 'borrowers' } },
  { path: 'products', component: BusinessLoanWorkspaceComponent, data: { tab: 'products' } },
  { path: 'applications', component: BusinessLoanWorkspaceComponent, data: { tab: 'applications' } },
  { path: 'approval', component: BusinessLoanWorkspaceComponent, data: { tab: 'approval' } },
  { path: 'servicing', component: BusinessLoanWorkspaceComponent, data: { tab: 'servicing' } },
  { path: '**', redirectTo: 'overview' }
];
