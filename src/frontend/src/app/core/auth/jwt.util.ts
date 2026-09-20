import { DecodedToken } from '../models/auth.model';

/**
 * Decodifica el payload de un JWT sin validar la firma (la validación real
 * la hace el backend en cada request). Solo se usa para leer claims no
 * sensibles (email, rol, permisos, exp) y decidir qué mostrar en la UI.
 */
export function decodeJwt(token: string): DecodedToken | null {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json) as DecodedToken;
  } catch {
    return null;
  }
}

export function isTokenExpired(decoded: DecodedToken): boolean {
  return Date.now() >= decoded.exp * 1000;
}
