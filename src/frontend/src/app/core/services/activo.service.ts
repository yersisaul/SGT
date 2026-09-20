import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { ActivoRequest, ActivoResponse } from '../models/activo.model';

/** CRUD real de Activo (ActivoController), para Administración. Distinto de
 * CatalogoService.getActivos(), que es de solo lectura y se usa como
 * catálogo de referencia en Solicitudes. */
@Injectable({ providedIn: 'root' })
export class ActivoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<ActivoResponse[]> {
    return this.http.get<ActivoResponse[]>(`${this.apiBaseUrl}/activos`);
  }

  crear(request: ActivoRequest): Observable<ActivoResponse> {
    return this.http.post<ActivoResponse>(`${this.apiBaseUrl}/activos`, request);
  }

  editar(id: string, request: ActivoRequest): Observable<ActivoResponse> {
    return this.http.put<ActivoResponse>(`${this.apiBaseUrl}/activos/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/activos/${id}`);
  }
}
