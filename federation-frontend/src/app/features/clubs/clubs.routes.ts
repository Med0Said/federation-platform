import { Routes } from '@angular/router';
export const clubsRoutes: Routes = [
  { path: '', loadComponent: () => import('./clubs-list/clubs-list.component').then(m => m.ClubsListComponent), data: { breadcrumb: 'Clubs' } },
  { path: ':id', loadComponent: () => import('./club-detail/club-detail.component').then(m => m.ClubDetailComponent), data: { breadcrumb: 'Details' } },
];
