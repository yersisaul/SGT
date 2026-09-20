import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { GenerarOrdenRequest, OrdenResponse } from '../models/orden.model';
import {
  HistorialRequerimientoResponse,
  RequerimientoRequest,
  RequerimientoResponse,
} from '../models/requerimiento.model';

/**
 * CRUD real de Requerimiento (RequerimientoController) + generación de OT
 * (RequerimientoController.generarOrdenDesdeRequerimiento). El PUT genérico
 * solo mueve entre Pendiente/En revisión; Aprobado/Rechazado se alcanzan
 * exclusivamente vía AprobacionService.
 */
@Injectable({ providedIn: 'root' })
export class RequerimientoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<RequerimientoResponse[]> {
    return this.http.get<RequerimientoResponse[]>(`${this.apiBaseUrl}/requerimientos`);
  }

  obtener(id: string): Observable<RequerimientoResponse> {
    return this.http.get<RequerimientoResponse>(`${this.apiBaseUrl}/requerimientos/${id}`);
  }

  crear(request: RequerimientoRequest): Observable<RequerimientoResponse> {
    return this.http.post<RequerimientoResponse>(`${this.apiBaseUrl}/requerimientos`, request);
  }

  editar(id: string, request: RequerimientoRequest): Observable<RequerimientoResponse> {
    return this.http.put<RequerimientoResponse>(`${this.apiBaseUrl}/requerimientos/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/requerimientos/${id}`);
  }

  /** POST /requerimientos/{id}/generar-orden — solo válido si el Requerimiento
   * está Aprobado; el backend lo valida igual (409 si no). */
  generarOrden(id: string, request: GenerarOrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/requerimientos/${id}/generar-orden`, request);
  }

  /** No existe endpoint filtrado por Requerimiento; se trae todo
   * (HistorialRequerimientoController) y se filtra en cliente. Requiere
   * historial_requerimiento.read. */
  listarHistorial(idRequerimiento: string): Observable<HistorialRequerimientoResponse[]> {
    return this.http
      .get<HistorialRequerimientoResponse[]>(`${this.apiBaseUrl}/historial-requerimientos`)
      .pipe(map((registros) => registros.filter((r) => r.id_requerimiento === idRequerimiento)));
  }
}
