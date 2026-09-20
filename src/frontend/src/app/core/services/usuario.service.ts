import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { UsuarioRequest, UsuarioResponse } from '../models/usuario.model';

/** CRUD real de Usuario (UsuarioController), para el módulo de Administración.
 * Distinto de CatalogoService.getUsuarios(), que es de solo lectura y se usa
 * como catálogo de referencia en Solicitudes/Requerimientos/Órdenes. */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar(): Observable<UsuarioResponse[]> {
    return this.http.get<UsuarioResponse[]>(`${this.apiBaseUrl}/usuarios`);
  }

  crear(request: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.apiBaseUrl}/usuarios`, request);
  }

  editar(id: string, request: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.apiBaseUrl}/usuarios/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/usuarios/${id}`);
  }
}
