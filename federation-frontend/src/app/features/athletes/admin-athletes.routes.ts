import { Routes } from '@angular/router';
export const adminAthletesRoutes: Routes = [
  { path: '', loadComponent: () => import('./athletes-list/athletes-list.component').then(m => m.AthletesListComponent), data: { breadcrumb: 'Athletes' } },
  { path: 'new', loadComponent: () => import('./athlete-form/athlete-form.component').then(m => m.AthleteFormComponent), data: { breadcrumb: 'New Athlete' } },
  { path: ':id', loadComponent: () => import('./athlete-detail/athlete-detail.component').then(m => m.AthleteDetailComponent), data: { breadcrumb: 'Details' } },
  { path: ':id/edit', loadComponent: () => import('./athlete-form/athlete-form.component').then(m => m.AthleteFormComponent), data: { breadcrumb: 'Edit' } },
];
