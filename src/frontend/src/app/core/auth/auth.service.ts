import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { AuthResponse, LoginRequest, SessionUser } from '../models/auth.model';
import { decodeJwt, isTokenExpired } from './jwt.util';
import { ModuloOperativo, PERMISOS_DE_MODULO } from './permisos-base';

const TOKEN_KEY = 'sgt_access_token';
const USER_KEY = 'sgt_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly currentUser = signal<SessionUser | null>(this.readStoredSession());

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly permisos = computed(() => this.currentUser()?.permisos ?? []);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBaseUrl}/auth/login`, request).pipe(
      tap((response) => this.setSession(response)),
    );
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.isBrowser ? localStorage.getItem(TOKEN_KEY) : null;
  }

  hasPermission(permission: string): boolean {
    return this.permisos().includes(permission);
  }

  /** Menú/acceso de un módulo operativo: requiere poder actuar sobre él (ver PERMISOS_DE_MODULO). */
  puedeOperarModulo(modulo: ModuloOperativo): boolean {
    return PERMISOS_DE_MODULO[modulo].some((permiso) => this.hasPermission(permiso));
  }

  private setSession(response: AuthResponse): void {
    const decoded = decodeJwt(response.accessToken);
    const sessionUser: SessionUser = {
      id: response.user.id,
      email: response.user.email,
      nombres: response.user.nombres,
      apellidos: response.user.apellidos,
      rol: decoded?.rol ?? response.user.role,
      permisos: decoded?.permisos ?? [],
    };

    if (this.isBrowser) {
      localStorage.setItem(TOKEN_KEY, response.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(sessionUser));
    }
    this.currentUser.set(sessionUser);
  }

  private clearSession(): void {
    if (this.isBrowser) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
    this.currentUser.set(null);
  }

  private readStoredSession(): SessionUser | null {
    if (!this.isBrowser) {
      return null;
    }
    const token = localStorage.getItem(TOKEN_KEY);
    const storedUser = localStorage.getItem(USER_KEY);
    if (!token || !storedUser) {
      return null;
    }
    const decoded = decodeJwt(token);
    if (!decoded || isTokenExpired(decoded)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      return null;
    }
    try {
      return JSON.parse(storedUser) as SessionUser;
    } catch {
      return null;
    }
  }
}
