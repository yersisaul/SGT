import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import {
  AsignacionOrdenResponse,
  AsignarOrdenRequest,
  CargaMiembroResponse,
  CerrarOrdenRequest,
  OrdenEscaladaResponse,
  HistorialOrdenResponse,
  OrdenRequest,
  OrdenResponse,
  ReasignarOrdenRequest,
  VerificarOrdenRequest,
} from '../models/orden.model';

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

  /** PUT genérico: el backend solo aplica id_estado (transición validada);
   * el resto del body se ignora (ver OrdenRequest). El adjunto se gestiona
   * aparte, vía ArchivoService. */
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

  /** POST /ordenes/{id}/reasignar — cambia el ejecutor asignado (orden.usuario).
   * Autorizado en backend solo al ejecutor actual o a un Administrador. */
  reasignar(id: string, request: ReasignarOrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}/reasignar`, request);
  }

  /** Historial de una Orden (filtrado en el backend por id_orden, validando
   * su visibilidad). Requiere historial_orden.read. */
  listarHistorial(idOrden: string): Observable<HistorialOrdenResponse[]> {
    return this.http.get<HistorialOrdenResponse[]>(`${this.apiBaseUrl}/historial-ordenes`, {
      params: { id_orden: idOrden },
    });
  }

  // ---- Cola de OT por especialidad (pasos 10-12) ----

  /** OT en cola de mis especialidades (y "Devuelta" si soy responsable). */
  /** OT que superaron el umbral de espera en cola (responsables y Administrador). */
  escaladas(): Observable<OrdenEscaladaResponse[]> {
    return this.http.get<OrdenEscaladaResponse[]>(`${this.apiBaseUrl}/ordenes/escaladas`);
  }

  cola(): Observable<OrdenResponse[]> {
    return this.http.get<OrdenResponse[]>(`${this.apiBaseUrl}/ordenes/cola`);
  }

  cargaEquipo(idEspecialidad: string): Observable<CargaMiembroResponse[]> {
    return this.http.get<CargaMiembroResponse[]>(`${this.apiBaseUrl}/ordenes/equipo/${idEspecialidad}`);
  }

  asignaciones(id: string): Observable<AsignacionOrdenResponse[]> {
    return this.http.get<AsignacionOrdenResponse[]>(`${this.apiBaseUrl}/ordenes/${id}/asignaciones`);
  }

  tomar(id: string): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}/tomar`, null);
  }

  asignar(id: string, request: AsignarOrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}/asignar`, request);
  }

  verificar(id: string, request: VerificarOrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.apiBaseUrl}/ordenes/${id}/verificar`, request);
  }
}
