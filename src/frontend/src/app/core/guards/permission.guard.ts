import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

/**
 * Protege una ruta según un permiso (route.data['permission']). Es una
 * capa de UX (oculta/bloquea navegación); la autorización real la valida
 * siempre el backend con @PreAuthorize .
 */
export const permissionGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredPermission = route.data['permission'] as string | undefined;

  if (!requiredPermission || authService.hasPermission(requiredPermission)) {
    return true;
  }

  return router.createUrlTree(['/app/dashboard']);
};
