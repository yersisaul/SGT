import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { ConfiguracionSlaRequest, ConfiguracionSlaResponse } from '../models/sla.model';

/** SLA de despacho y atención editables (sla.read / sla.update). */
@Injectable({ providedIn: 'root' })
export class ConfiguracionSlaService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<ConfiguracionSlaResponse[]> {
    return this.http.get<ConfiguracionSlaResponse[]>(`${this.apiBaseUrl}/configuracion/sla`);
  }

  actualizar(request: ConfiguracionSlaRequest): Observable<ConfiguracionSlaResponse[]> {
    return this.http.put<ConfiguracionSlaResponse[]>(`${this.apiBaseUrl}/configuracion/sla`, request);
  }
}
