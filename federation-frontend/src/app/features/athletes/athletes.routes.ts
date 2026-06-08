import { Routes } from '@angular/router';
export const athletesRoutes: Routes = [
  { path: '', loadComponent: () => import('./athletes-list/athletes-list.component').then(m => m.AthletesListComponent), data: { breadcrumb: 'Athletes' } },
  { path: ':id', loadComponent: () => import('./athlete-detail/athlete-detail.component').then(m => m.AthleteDetailComponent), data: { breadcrumb: 'Details' } },
];
