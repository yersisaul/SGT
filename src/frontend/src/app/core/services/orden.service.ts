import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { CerrarOrdenRequest, HistorialOrdenResponse, OrdenRequest, OrdenResponse } from '../models/orden.model';

/**
 * CRUD real de Orden (OrdenController) + cierre (cerrarOrden). No hay
 * creación desde acá: nadie tiene orden.create — una Orden solo nace de
 * SolicitudService.generarOrden()/RequerimientoService.generarOrden().
 */
@Injectable({ providedIn: 'root' })
export class OrdenService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<OrdenResponse[]> {
    return this.http.get<OrdenResponse[]>(`${this.apiBaseUrl}/ordenes`);
  }

  obtener(id: string): Observable<OrdenResponse> {
    return this.http.get<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}`);
  }

  /** PUT genérico: el backend solo aplica id_estado (transición validada) y
   * url_adjunto; el resto del body se ignora (ver OrdenRequest). */
  editar(id: string, request: OrdenRequest): Observable<OrdenResponse> {
    return this.http.put<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/ordenes/${id}`);
  }

  /** POST /ordenes/{id}/cerrar — único camino a Finalizado. */
  cerrar(id: string, request: CerrarOrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}/cerrar`, request);
  }

  /** No existe endpoint filtrado por Orden; se trae todo
   * (HistorialOrdenController) y se filtra en cliente. Requiere
   * historial_orden.read. */
  listarHistorial(idOrden: string): Observable<HistorialOrdenResponse[]> {
    return this.http
      .get<HistorialOrdenResponse[]>(`${this.apiBaseUrl}/historial-ordenes`)
      .pipe(map((registros) => registros.filter((r) => r.id_orden === idOrden)));
  }
}
