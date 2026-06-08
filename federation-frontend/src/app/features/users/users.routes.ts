import { Routes } from '@angular/router';
export const usersRoutes: Routes = [
  { path: '', loadComponent: () => import('./users-list/users-list.component').then(m => m.UsersListComponent), data: { breadcrumb: 'Users' } },
  { path: ':id', loadComponent: () => import('./user-detail/user-detail.component').then(m => m.UserDetailComponent), data: { breadcrumb: 'Details' } },
];
