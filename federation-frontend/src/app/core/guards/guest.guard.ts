import { inject }       from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { UserRole }    from '@core/models';

/**
 * GuestGuard — redirects already-authenticated users away from public-only
 * routes (login, register) to the appropriate dashboard.
 *
 * Usage:
 *   { path: 'login', canActivate: [guestGuard], ... }
 */
export const guestGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return true;
  }

  // Redirect to role-appropriate home
  return router.createUrlTree([getHomeForRole(auth.userRole())]);
};

function getHomeForRole(role: UserRole | null): string {
  switch (role) {
    case UserRole.ADMIN:
    case UserRole.FEDERATION_STAFF:
      return '/admin/dashboard';
    case UserRole.CLUB_MANAGER:
      return '/admin/clubs';
    case UserRole.ATHLETE:
      return '/portal/profile';
    default:
      return '/';
  }
}
