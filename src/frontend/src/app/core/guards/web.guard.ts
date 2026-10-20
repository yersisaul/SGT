import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { RUTA_LOGIN_WEB } from '../auth/permisos-web';

/**
 * Panel del sitio web: exige sesión y algún permiso web_*.read. Sin sesión
 * lleva al login propio del panel (no al del SGT). Es una capa de UX: el
 * backend valida cada operación con @PreAuthorize.
 */
export const webGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree([RUTA_LOGIN_WEB], { queryParams: { returnUrl: state.url } });
  }
  if (!authService.puedeAdministrarWeb()) {
    return router.createUrlTree([RUTA_LOGIN_WEB], { queryParams: { sinAcceso: 1 } });
  }
  return true;
};

/** Sección del panel: route.data['permission'] (web_*.read); sin él vuelve al resumen. */
export const webPermissionGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requerido = route.data['permission'] as string | undefined;
  return !requerido || authService.hasPermission(requerido) ? true : router.createUrlTree(['/web/resumen']);
};
