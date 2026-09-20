import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { EspecialidadRequest, EspecialidadResponse } from '../models/especialidad.model';

/** CRUD real de Especialidad (EspecialidadController), para Administración.
 * Distinto de CatalogoService.getEspecialidades(), que es de solo lectura. */
@Injectable({ providedIn: 'root' })
export class EspecialidadService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<EspecialidadResponse[]> {
    return this.http.get<EspecialidadResponse[]>(`${this.apiBaseUrl}/especialidades`);
  }

  crear(request: EspecialidadRequest): Observable<EspecialidadResponse> {
    return this.http.post<EspecialidadResponse>(`${this.apiBaseUrl}/especialidades`, request);
  }

  editar(id: string, request: EspecialidadRequest): Observable<EspecialidadResponse> {
    return this.http.put<EspecialidadResponse>(`${this.apiBaseUrl}/especialidades/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/especialidades/${id}`);
  }
}
