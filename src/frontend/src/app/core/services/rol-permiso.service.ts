import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { RolPermisoRequest, RolPermisoResponse } from '../models/rol-permiso.model';

/** Asignación de Permiso a Rol (RolPermisoController). Solo create + read +
 * delete: no existe "editar" una asociación, se revoca y se vuelve a asignar
 * (ver comentario del controller real en backend).
 *
 * No existe un endpoint filtrado por rol (GET /rol-permisos/rol/{id}); se
 * trae la lista completa y se filtra en cliente por id_rol, igual que
 * OrdenService.listarHistorial con /historial-ordenes. */
@Injectable({ providedIn: 'root' })
export class RolPermisoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<RolPermisoResponse[]> {
    return this.http.get<RolPermisoResponse[]>(`${this.apiBaseUrl}/rol-permisos`);
  }

  asignar(request: RolPermisoRequest): Observable<RolPermisoResponse> {
    return this.http.post<RolPermisoResponse>(`${this.apiBaseUrl}/rol-permisos`, request);
  }

  revocar(idRolPermiso: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/rol-permisos/${idRolPermiso}`);
  }
}
