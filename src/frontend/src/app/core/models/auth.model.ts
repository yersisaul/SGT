/** Contratos reales de POST /api/auth/login (ver AuthController/AuthServiceImpl del backend). */

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: string;
  email: string;
  nombres: string;
  apellidos: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string | null;
  user: LoginResponse;
}

/** Claims del JWT emitidas por JwtService (backend): sub, email, rol, permisos, iat, exp. */
export interface DecodedToken {
  sub: string;
  email: string;
  rol: string;
  permisos: string[];
  iat: number;
  exp: number;
}

/**
 * Vista de sesión usada por la UI: combina lo que trae el JWT (autoridad —
 * rol/permisos, siempre vigente) con el perfil que solo devuelve el login
 * (nombres/apellidos, el JWT no los incluye). Se persiste junto al token
 * para sobrevivir un recargo de página.
 */
export interface SessionUser {
  id: string;
  email: string;
  nombres: string;
  apellidos: string;
  rol: string;
  permisos: string[];
}

