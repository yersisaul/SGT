import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { EstadoRequest, EstadoResponse } from '../models/estado.model';

/** CRUD real de Estado (EstadoController), para Administración. Distinto de
 * CatalogoService.getEstados(), que es de solo lectura y degrada a lista
 * vacía en error (no sirve para una pantalla de gestión). */
@Injectable({ providedIn: 'root' })
export class EstadoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<EstadoResponse[]> {
    return this.http.get<EstadoResponse[]>(`${this.apiBaseUrl}/estados`);
  }

  crear(request: EstadoRequest): Observable<EstadoResponse> {
    return this.http.post<EstadoResponse>(`${this.apiBaseUrl}/estados`, request);
  }

  editar(id: string, request: EstadoRequest): Observable<EstadoResponse> {
    return this.http.put<EstadoResponse>(`${this.apiBaseUrl}/estados/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/estados/${id}`);
  }
}
