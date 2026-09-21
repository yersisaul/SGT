import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { ActivoCatalogo, EspecialidadCatalogo, EstadoCatalogo, RolCatalogo, UsuarioCatalogo } from '../models/catalogo.model';

/**
 * Catálogos de referencia (Estado/Activo/Especialidad/Usuario) usados por
 * Solicitudes y futuros módulos (Requerimientos, Órdenes) que comparten las
 * mismas relaciones. Cada permiso (estado.read, activo.read, ...) es
 * independiente de solicitud.read: si el usuario no lo tiene, se degrada a
 * lista vacía en vez de romper la pantalla que lo consume.
 */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getEstados(): Observable<EstadoCatalogo[]> {
    return this.http
      .get<EstadoCatalogo[]>(`${this.apiBaseUrl}/estados`)
      .pipe(catchError(() => of([])));
  }

  getActivos(): Observable<ActivoCatalogo[]> {
    return this.http
      .get<ActivoCatalogo[]>(`${this.apiBaseUrl}/activos`)
      .pipe(catchError(() => of([])));
  }

  getEspecialidades(): Observable<EspecialidadCatalogo[]> {
    return this.http
      .get<EspecialidadCatalogo[]>(`${this.apiBaseUrl}/especialidades`)
      .pipe(catchError(() => of([])));
  }

  getUsuarios(): Observable<UsuarioCatalogo[]> {
    return this.http
      .get<UsuarioCatalogo[]>(`${this.apiBaseUrl}/usuarios`)
      .pipe(catchError(() => of([])));
  }

  /** Usuarios de rol Operaciones, para el selector de ejecutor al generar o
   * reasignar una Orden. A diferencia de getUsuarios() (requiere
   * usuario.read, que Despachador/Operaciones no tienen), este endpoint está
   * autorizado a quien puede generar_orden o reasignar (ver UsuarioController). */
  getUsuariosOperaciones(): Observable<UsuarioCatalogo[]> {
    return this.http
      .get<UsuarioCatalogo[]>(`${this.apiBaseUrl}/usuarios/operaciones`)
      .pipe(catchError(() => of([])));
  }

  getRoles(): Observable<RolCatalogo[]> {
    return this.http
      .get<RolCatalogo[]>(`${this.apiBaseUrl}/roles`)
      .pipe(catchError(() => of([])));
  }
}
