import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { PermisoRequest, PermisoResponse } from '../models/permiso.model';

/** CRUD real de Permiso (PermisoController). */
@Injectable({ providedIn: 'root' })
export class PermisoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<PermisoResponse[]> {
    return this.http.get<PermisoResponse[]>(`${this.apiBaseUrl}/permisos`);
  }

  crear(request: PermisoRequest): Observable<PermisoResponse> {
    return this.http.post<PermisoResponse>(`${this.apiBaseUrl}/permisos`, request);
  }

  editar(id: string, request: PermisoRequest): Observable<PermisoResponse> {
    return this.http.put<PermisoResponse>(`${this.apiBaseUrl}/permisos/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/permisos/${id}`);
  }
}
