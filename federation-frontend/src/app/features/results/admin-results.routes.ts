import { Routes } from '@angular/router';
export const adminResultsRoutes: Routes = [
  { path: '', loadComponent: () => import('./results-list/results-list.component').then(m => m.ResultsListComponent), data: { breadcrumb: 'Results' } },
  { path: 'new', loadComponent: () => import('./result-form/result-form.component').then(m => m.ResultFormComponent), data: { breadcrumb: 'Enter Results' } },
  { path: ':id', loadComponent: () => import('./result-detail/result-detail.component').then(m => m.ResultDetailComponent), data: { breadcrumb: 'Details' } },
  { path: ':id/edit', loadComponent: () => import('./result-form/result-form.component').then(m => m.ResultFormComponent), data: { breadcrumb: 'Edit' } },
];
