import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { AprobacionRequest, AprobacionResponse } from '../models/aprobacion.model';

/**
 * AprobacionController: solo Create + Read (es un registro de un evento de
 * negocio ya ocurrido). El POST es la operación real de aprobar/rechazar un
 * Requerimiento (gateado en backend por requerimiento.aprobar, no
 * aprobacion.create). No existe endpoint filtrado por Requerimiento; se
 * trae todo y se filtra en cliente. Requiere aprobacion.read para leer.
 */
@Injectable({ providedIn: 'root' })
export class AprobacionService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  crear(request: AprobacionRequest): Observable<AprobacionResponse> {
    return this.http.post<AprobacionResponse>(`${this.apiBaseUrl}/aprobaciones`, request);
  }

  listarPorRequerimiento(idRequerimiento: string): Observable<AprobacionResponse[]> {
    return this.http
      .get<AprobacionResponse[]>(`${this.apiBaseUrl}/aprobaciones`)
      .pipe(map((registros) => registros.filter((r) => r.id_requerimiento === idRequerimiento)));
  }
}
