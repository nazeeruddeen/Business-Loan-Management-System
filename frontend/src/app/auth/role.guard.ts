import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  const allowedRoles = (route.data?.['roles'] as string[] | undefined)?.map((r) => r.toUpperCase()) ?? [];
  if (allowedRoles.length === 0) {
    return true;
  }

  const currentRole = authService.getRole()?.toUpperCase();
  if (currentRole && allowedRoles.includes(currentRole)) {
    return true;
  }

  router.navigate(['/']);
  return false;
};
