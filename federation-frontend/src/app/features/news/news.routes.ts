import { Routes } from '@angular/router';
export const newsRoutes: Routes = [
  { path: '', loadComponent: () => import('./news-list/news-list.component').then(m => m.NewsListComponent), data: { breadcrumb: 'News' } },
  { path: ':slug', loadComponent: () => import('./news-detail/news-detail.component').then(m => m.NewsDetailComponent), data: { breadcrumb: 'Article' } },
];
