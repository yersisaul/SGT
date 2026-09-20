import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { NotificationService } from '../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../shared/utils/api-error.util';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const isLoginRequest = req.url.includes('/auth/login');

        if (error.status === 401 && !isLoginRequest) {
          authService.logout();
          notifications.error('Tu sesión expiró. Inicia sesión nuevamente.');
        } else if (error.status === 403) {
          notifications.error('No tienes permiso para realizar esta operación.');
        } else if (error.status === 429) {
          notifications.error(extractApiErrorMessage(error) || 'Demasiadas solicitudes, intenta más tarde.');
        } else if (error.status === 0) {
          notifications.error('No se pudo conectar con el servidor.');
        } else if (!isLoginRequest) {
          // Los errores del login se muestran inline en el propio formulario,
          // no como toast, para no duplicar el mensaje. Los 409 de reglas de
          // negocio (p.ej. generar-orden) caen acá con su mensaje real.
          notifications.error(extractApiErrorMessage(error));
        }
      }
      return throwError(() => error);
    }),
  );
};
