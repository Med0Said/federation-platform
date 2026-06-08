import { Routes } from '@angular/router';
export const adminNewsRoutes: Routes = [
  { path: '', loadComponent: () => import('./news-list/news-list.component').then(m => m.NewsListComponent), data: { breadcrumb: 'News' } },
  { path: 'new', loadComponent: () => import('./news-form/news-form.component').then(m => m.NewsFormComponent), data: { breadcrumb: 'New Article' } },
  { path: ':id/edit', loadComponent: () => import('./news-form/news-form.component').then(m => m.NewsFormComponent), data: { breadcrumb: 'Edit' } },
];
