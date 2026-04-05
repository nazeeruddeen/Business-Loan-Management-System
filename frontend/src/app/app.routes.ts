import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  { path: 'overview', loadComponent: () => import('./workspace.component').then((m) => m.BusinessLoanWorkspaceComponent), data: { tab: 'dashboard' } },
  { path: 'borrowers', loadComponent: () => import('./workspace.component').then((m) => m.BusinessLoanWorkspaceComponent), data: { tab: 'borrowers' } },
  { path: 'products', loadComponent: () => import('./workspace.component').then((m) => m.BusinessLoanWorkspaceComponent), data: { tab: 'products' } },
  { path: 'applications', loadComponent: () => import('./workspace.component').then((m) => m.BusinessLoanWorkspaceComponent), data: { tab: 'applications' } },
  { path: 'approval', loadComponent: () => import('./workspace.component').then((m) => m.BusinessLoanWorkspaceComponent), data: { tab: 'approval' } },
  { path: 'servicing', loadComponent: () => import('./workspace.component').then((m) => m.BusinessLoanWorkspaceComponent), data: { tab: 'servicing' } },
  { path: '**', redirectTo: 'overview' }
];
