import { inject }        from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService }  from '@core/services/auth.service';

/**
 * AuthGuard — blocks access to authenticated routes when no user is signed in.
 *
 * On failure: redirects to /auth/login, preserving the attempted URL as a
 * `returnUrl` query param so the user is sent back after login.
 *
 * Usage in routes:
 *   { path: 'dashboard', canActivate: [authGuard], ... }
 */
export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  // Preserve attempted URL for post-login redirect
  return router.createUrlTree(
    ['/auth/login'],
    { queryParams: { returnUrl: state.url } }
  );
};
