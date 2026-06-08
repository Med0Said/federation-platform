import { Routes } from '@angular/router';
import { authGuard }  from '@core/guards/auth.guard';
import { roleGuard }  from '@core/guards/role.guard';
import { guestGuard } from '@core/guards/guest.guard';
import { UserRole }   from '@core/models';

export const routes: Routes = [

  // ══════════════════════════════════════════════════════════════════════════
  // DEFAULT — redirect based on auth state
  // ══════════════════════════════════════════════════════════════════════════
  {
    path: '',
    pathMatch: 'full',
    redirectTo: '/home',
  },

  // ══════════════════════════════════════════════════════════════════════════
  // PUBLIC LAYOUT — unauthenticated visitors
  // ══════════════════════════════════════════════════════════════════════════
  {
    path: '',
    loadComponent: () =>
      import('./layouts/public-layout/public-layout.component')
        .then(m => m.PublicLayoutComponent),
    children: [
      {
        path: 'home',
        loadComponent: () =>
          import('./features/home/home.component')
            .then(m => m.HomeComponent),
        title: 'Home — Sports Federation',
      },
      {
        path: 'competitions',
        loadChildren: () =>
          import('./features/competitions/competitions.routes')
            .then(m => m.competitionsRoutes),
        title: 'Competitions',
      },
      {
        path: 'clubs',
        loadChildren: () =>
          import('./features/clubs/clubs.routes')
            .then(m => m.clubsRoutes),
        title: 'Clubs',
      },
      {
        path: 'athletes',
        loadChildren: () =>
          import('./features/athletes/athletes.routes')
            .then(m => m.athletesRoutes),
        title: 'Athletes',
      },
      {
        path: 'news',
        loadChildren: () =>
          import('./features/news/news.routes')
            .then(m => m.newsRoutes),
        title: 'News',
      },
      {
        path: 'results',
        loadChildren: () =>
          import('./features/results/results.routes')
            .then(m => m.resultsRoutes),
        title: 'Results',
      },
    ],
  },

  // ══════════════════════════════════════════════════════════════════════════
  // AUTH LAYOUT — login / register (guests only)
  // ══════════════════════════════════════════════════════════════════════════
  {
    path: 'auth',
    loadComponent: () =>
      import('./layouts/public-layout/public-layout.component')
        .then(m => m.PublicLayoutComponent),
    children: [
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/login/login.component')
            .then(m => m.LoginComponent),
        title: 'Login',
      },
      {
        path: 'register',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/register/register.component')
            .then(m => m.RegisterComponent),
        title: 'Register',
      },
      {
        path: 'forgot-password',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/forgot-password/forgot-password.component')
            .then(m => m.ForgotPasswordComponent),
        title: 'Forgot Password',
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'login',
      },
    ],
  },

  // ══════════════════════════════════════════════════════════════════════════
  // ADMIN LAYOUT — authenticated users with elevated roles
  // ══════════════════════════════════════════════════════════════════════════
  {
    path: 'admin',
    loadComponent: () =>
      import('./layouts/admin-layout/admin-layout.component')
        .then(m => m.AdminLayoutComponent),
    canActivate: [authGuard],
    children: [

      // ── Dashboard ───────────────────────────────────────────────────────
      {
        path: 'dashboard',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER] },
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent),
        title: 'Dashboard',
      },

      // ── Users (Admin only) ──────────────────────────────────────────────
      {
        path: 'users',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN] },
        loadChildren: () =>
          import('./features/users/admin-users.routes')
            .then(m => m.adminUsersRoutes),
        title: 'Users',
      },

      // ── Clubs ────────────────────────────────────────────────────────────
      {
        path: 'clubs',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER] },
        loadChildren: () =>
          import('./features/clubs/admin-clubs.routes')
            .then(m => m.adminClubsRoutes),
        title: 'Manage Clubs',
      },

      // ── Athletes ─────────────────────────────────────────────────────────
      {
        path: 'athletes',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER] },
        loadChildren: () =>
          import('./features/athletes/admin-athletes.routes')
            .then(m => m.adminAthletesRoutes),
        title: 'Manage Athletes',
      },

      // ── Competitions ─────────────────────────────────────────────────────
      {
        path: 'competitions',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
        loadChildren: () =>
          import('./features/competitions/admin-competitions.routes')
            .then(m => m.adminCompetitionsRoutes),
        title: 'Manage Competitions',
      },

      // ── Results ──────────────────────────────────────────────────────────
      {
        path: 'results',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
        loadChildren: () =>
          import('./features/results/admin-results.routes')
            .then(m => m.adminResultsRoutes),
        title: 'Manage Results',
      },

      // ── News ─────────────────────────────────────────────────────────────
      {
        path: 'news',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
        loadChildren: () =>
          import('./features/news/admin-news.routes')
            .then(m => m.adminNewsRoutes),
        title: 'Manage News',
      },

      // ── Profile (all authenticated users) ────────────────────────────────
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component')
            .then(m => m.ProfileComponent),
        title: 'My Profile',
      },

      // Default admin redirect
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
    ],
  },

  // ══════════════════════════════════════════════════════════════════════════
  // ATHLETE PORTAL — self-service for athletes
  // ══════════════════════════════════════════════════════════════════════════
  {
    path: 'portal',
    loadComponent: () =>
      import('./layouts/admin-layout/admin-layout.component')
        .then(m => m.AdminLayoutComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: [UserRole.ATHLETE, UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
    children: [
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component')
            .then(m => m.ProfileComponent),
        title: 'My Profile',
      },
      {
        path: 'registrations',
        loadComponent: () =>
          import('./features/portal/my-registrations/my-registrations.component')
            .then(m => m.MyRegistrationsComponent),
        title: 'My Registrations',
      },
      {
        path: 'results',
        loadComponent: () =>
          import('./features/portal/my-results/my-results.component')
            .then(m => m.MyResultsComponent),
        title: 'My Results',
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'profile',
      },
    ],
  },

  // ══════════════════════════════════════════════════════════════════════════
  // ERROR PAGES
  // ══════════════════════════════════════════════════════════════════════════
  {
    path: '403',
    loadComponent: () =>
      import('./features/errors/forbidden/forbidden.component')
        .then(m => m.ForbiddenComponent),
    title: '403 — Forbidden',
  },
  {
    path: '404',
    loadComponent: () =>
      import('./features/errors/not-found/not-found.component')
        .then(m => m.NotFoundComponent),
    title: '404 — Not Found',
  },

  // Wildcard → 404
  {
    path: '**',
    redirectTo: '/404',
  },
];
