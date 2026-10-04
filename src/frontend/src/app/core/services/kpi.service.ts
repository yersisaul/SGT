import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { FamiliaKpi, KpiResponse, RangoKpi } from '../models/kpi.model';

/** KPIs del flujo (PRD E6). El alcance (global o especialidades del responsable) lo decide el backend. */
@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  obtener(familia: FamiliaKpi, rango: RangoKpi): Observable<KpiResponse> {
    return this.http.get<KpiResponse>(`${this.apiBaseUrl}/kpis/${familia}`, { params: { ...rango } });
  }

  /** CSV vía HttpClient (el interceptor agrega el JWT); requiere kpi.export. */
  exportarCsv(familia: FamiliaKpi, rango: RangoKpi): Observable<Blob> {
    return this.http.get(`${this.apiBaseUrl}/kpis/${familia}/export.csv`, {
      params: { ...rango },
      responseType: 'blob',
    });
  }
}
