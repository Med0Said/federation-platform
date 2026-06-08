import { Routes } from '@angular/router';
export const adminClubsRoutes: Routes = [
  { path: '', loadComponent: () => import('./clubs-list/clubs-list.component').then(m => m.ClubsListComponent), data: { breadcrumb: 'Clubs' } },
  { path: 'new', loadComponent: () => import('./club-form/club-form.component').then(m => m.ClubFormComponent), data: { breadcrumb: 'New Club' } },
  { path: ':id', loadComponent: () => import('./club-detail/club-detail.component').then(m => m.ClubDetailComponent), data: { breadcrumb: 'Details' } },
  { path: ':id/edit', loadComponent: () => import('./club-form/club-form.component').then(m => m.ClubFormComponent), data: { breadcrumb: 'Edit' } },
];
