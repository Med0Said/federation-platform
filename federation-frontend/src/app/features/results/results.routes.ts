import { Routes } from '@angular/router';
export const resultsRoutes: Routes = [
  { path: '', loadComponent: () => import('./results-list/results-list.component').then(m => m.ResultsListComponent), data: { breadcrumb: 'Results' } },
  { path: ':id', loadComponent: () => import('./result-detail/result-detail.component').then(m => m.ResultDetailComponent), data: { breadcrumb: 'Details' } },
];
