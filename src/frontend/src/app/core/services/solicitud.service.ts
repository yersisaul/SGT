import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { GenerarOrdenRequest, OrdenResponse } from '../models/orden.model';
import { RequerimientoResponse } from '../models/requerimiento.model';
import {
  GenerarRequerimientoRequest,
  HistorialSolicitudRequest,
  HistorialSolicitudResponse,
  SolicitudRequest,
  SolicitudResponse,
} from '../models/solicitud.model';

/**
 * CRUD real de Solicitud (SolicitudController) + operaciones de negocio
 * (SolicitudController.generarOrdenDesdeSolicitud). El PUT genérico sigue
 * existiendo para las transiciones Pendiente/En revisión/En progreso; llegar
 * a Finalizado es exclusivo de generarOrden().
 */
@Injectable({ providedIn: 'root' })
export class SolicitudService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<SolicitudResponse[]> {
    return this.http.get<SolicitudResponse[]>(`${this.apiBaseUrl}/solicitudes`);
  }

  obtener(id: string): Observable<SolicitudResponse> {
    return this.http.get<SolicitudResponse>(`${this.apiBaseUrl}/solicitudes/${id}`);
  }

  crear(request: SolicitudRequest): Observable<SolicitudResponse> {
    return this.http.post<SolicitudResponse>(`${this.apiBaseUrl}/solicitudes`, request);
  }

  editar(id: string, request: SolicitudRequest): Observable<SolicitudResponse> {
    return this.http.put<SolicitudResponse>(`${this.apiBaseUrl}/solicitudes/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/solicitudes/${id}`);
  }

  /** POST /solicitudes/{id}/generar-orden — camino "bajo contrato" (Pendiente -> En progreso). */
  generarOrden(id: string, request: GenerarOrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/solicitudes/${id}/generar-orden`, request);
  }

  /** POST /solicitudes/{id}/generar-requerimiento — camino "fuera de contrato"
   * (Pendiente -> En revisión). Ambos son las dos ramas de la misma decisión
   * de clasificación del Despachador; solo una puede ejecutarse. */
  generarRequerimiento(id: string, request: GenerarRequerimientoRequest): Observable<RequerimientoResponse> {
    return this.http.post<RequerimientoResponse>(`${this.apiBaseUrl}/solicitudes/${id}/generar-requerimiento`, request);
  }

  /** Bitácora de auditoría (HistorialSolicitudController). Se usa best-effort
   * tras una transición por PUT (Pendiente/En revisión/En progreso); requiere
   * historial_solicitud.create. generarOrden() registra su propio historial
   * en el backend, no se llama desde acá tras esa acción. */
  registrarHistorial(request: HistorialSolicitudRequest): Observable<unknown> {
    return this.http.post(`${this.apiBaseUrl}/historial-solicitudes`, request);
  }

  /** Historial de una Solicitud (filtrado en el backend por id_solicitud,
   * validando su visibilidad). Requiere historial_solicitud.read. */
  listarHistorial(idSolicitud: string): Observable<HistorialSolicitudResponse[]> {
    return this.http.get<HistorialSolicitudResponse[]>(`${this.apiBaseUrl}/historial-solicitudes`, { params: { id_solicitud: idSolicitud } });
  }
}
