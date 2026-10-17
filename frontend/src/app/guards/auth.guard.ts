import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
// El guard es navegación/UX. La autorización real siempre ocurre en Spring Security.
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.session()) return router.createUrlTree(['/login']);
  return auth.hasRole(route.data['role']) || router.createUrlTree(['/']);
};
