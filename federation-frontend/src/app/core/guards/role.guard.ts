import { inject }       from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { UserRole }    from '@core/models';

/**
 * RoleGuard — restricts routes to specific roles.
 *
 * Required roles are declared in the route's `data.roles` array:
 *
 *   {
 *     path: 'admin',
 *     canActivate: [authGuard, roleGuard],
 *     data: { roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
 *     ...
 *   }
 *
 * On failure: redirects to /403 (unauthorized page).
 * Note: always pair with authGuard to ensure the user is signed in first.
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as UserRole[] | undefined;

  // No roles defined on route → allow any authenticated user
  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  if (auth.hasAnyRole(requiredRoles)) {
    return true;
  }

  return router.createUrlTree(['/403']);
};
