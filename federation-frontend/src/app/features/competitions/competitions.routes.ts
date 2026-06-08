import { Routes } from '@angular/router';
// Feature pages will be implemented in the next phase
export const competitionsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./competitions-list/competitions-list.component')
        .then(m => m.CompetitionsListComponent),
    data: { breadcrumb: 'Competitions' },
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./competition-detail/competition-detail.component')
        .then(m => m.CompetitionDetailComponent),
    data: { breadcrumb: 'Details' },
  },
];
