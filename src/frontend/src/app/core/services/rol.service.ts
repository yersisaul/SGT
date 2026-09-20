import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { RolRequest, RolResponse } from '../models/rol.model';

/** CRUD real de Rol (RolController). La gestión de permisos asociados a un
 * Rol vive en RolPermisoService (no hay campo de permisos en RolRequest). */
@Injectable({ providedIn: 'root' })
export class RolService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<RolResponse[]> {
    return this.http.get<RolResponse[]>(`${this.apiBaseUrl}/roles`);
  }

  crear(request: RolRequest): Observable<RolResponse> {
    return this.http.post<RolResponse>(`${this.apiBaseUrl}/roles`, request);
  }

  editar(id: string, request: RolRequest): Observable<RolResponse> {
    return this.http.put<RolResponse>(`${this.apiBaseUrl}/roles/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/roles/${id}`);
  }
}
