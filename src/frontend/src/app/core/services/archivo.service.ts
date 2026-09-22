import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';

/** Recursos del backend que aceptan imagen/adjunto (ArchivoController). */
export type RecursoArchivo =
  | 'activos'
  | 'usuarios'
  | 'solicitudes'
  | 'requerimientos'
  | 'aprobaciones'
  | 'ordenes';

/**
 * Fileserver propio del backend SGT: sube, descarga y elimina los archivos
 * de las 6 entidades vía ArchivoController (/api/archivos/...). No usa
 * MinIO/S3 ni ningún servicio externo — el backend es quien almacena y sirve
 * los archivos en su filesystem.
 */
@Injectable({ providedIn: 'root' })
export class ArchivoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  subir<T = unknown>(recurso: RecursoArchivo, id: string, file: File): Observable<T> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<T>(`${this.apiBaseUrl}/archivos/${recurso}/${id}`, formData);
  }

  eliminar<T = unknown>(recurso: RecursoArchivo, id: string): Observable<T> {
    return this.http.delete<T>(`${this.apiBaseUrl}/archivos/${recurso}/${id}`);
  }

  /**
   * Descarga el binario de una ruta ya resuelta por el backend (el
   * url_img/url_adjunto de la entidad, p.ej. "/api/archivos/activos/{id}").
   * Se pide vía HttpClient (no <img src> directo) porque el endpoint exige
   * el JWT en el header Authorization, que el interceptor solo agrega a
   * peticiones hechas con HttpClient.
   */
  descargarBlob(rutaApi: string): Observable<Blob> {
    return this.http.get(this.resolverUrl(rutaApi), { responseType: 'blob' });
  }

  private resolverUrl(rutaApi: string): string {
    // apiBaseUrl ya incluye "/api" (ver API_BASE_URL); rutaApi también
    // empieza con "/api/...", así que solo se antepone el origen.
    const origen = this.apiBaseUrl.replace(/\/api\/?$/, '');
    return `${origen}${rutaApi}`;
  }
}
