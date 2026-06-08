import { Routes } from '@angular/router';
export const adminCompetitionsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./competitions-list/competitions-list.component')
        .then(m => m.CompetitionsListComponent),
    data: { breadcrumb: 'Competitions' },
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./competition-form/competition-form.component')
        .then(m => m.CompetitionFormComponent),
    data: { breadcrumb: 'New Competition' },
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./competition-detail/competition-detail.component')
        .then(m => m.CompetitionDetailComponent),
    data: { breadcrumb: 'Details' },
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./competition-form/competition-form.component')
        .then(m => m.CompetitionFormComponent),
    data: { breadcrumb: 'Edit' },
  },
];
