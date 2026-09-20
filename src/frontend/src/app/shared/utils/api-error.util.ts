import { HttpErrorResponse } from '@angular/common/http';

/** Formas posibles del cuerpo de error del backend: ProblemDetail
 * (detail, ver GlobalExceptionHandler.handleNotFound/handleDuplicate) o
 * ErrorResponse (message, ver GlobalExceptionHandler.handleResponseStatus —
 * el que usan las reglas de negocio lanzadas como ResponseStatusException,
 * p.ej. los 409 de generar-orden/cerrar). */
interface ApiErrorBody {
  detail?: string;
  message?: string;
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return typeof value === 'object' && value !== null;
}

/**
 * Único punto de extracción del mensaje real de error del backend. Lo usan
 * tanto el interceptor global (toast) como las pantallas que necesitan
 * mostrar el mensaje de negocio inline (p.ej. un 409 dentro de un diálogo de
 * confirmación) sin reimplementar el parseo del body en cada una.
 */
export function extractApiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body: unknown = error.error;
    if (isApiErrorBody(body)) {
      if (typeof body.detail === 'string') return body.detail;
      if (typeof body.message === 'string') return body.message;
    }
  }
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
}
