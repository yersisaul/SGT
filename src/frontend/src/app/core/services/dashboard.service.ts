import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { ResumenEstados } from '../models/dashboard.model';

/**
 * Resúmenes de solo lectura para el Dashboard. No es el servicio CRUD de
 * Solicitud/Requerimiento (eso llega en la fase de CRUD) — solo los dos
 * endpoints de agregación que consume esta pantalla.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getResumenSolicitudes(): Observable<ResumenEstados> {
    return this.http.get<ResumenEstados>(`${this.apiBaseUrl}/solicitudes/resumen-estados`);
  }

  getResumenRequerimientos(): Observable<ResumenEstados> {
    return this.http.get<ResumenEstados>(`${this.apiBaseUrl}/requerimientos/resumen-estados`);
  }
}
