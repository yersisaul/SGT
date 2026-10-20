import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import {
  EstadoNovedad,
  EstadoNovedadRequest,
  PaginaResponse,
  WebCasoExitoRequest,
  WebCasoExitoResponse,
  WebCategoriaNovedadRequest,
  WebCategoriaNovedadResponse,
  WebClienteReferenciaRequest,
  WebClienteReferenciaResponse,
  WebDocumentoRequest,
  WebDocumentoResponse,
  WebNovedadRequest,
  WebNovedadResponse,
  WebPartnerRequest,
  WebPartnerResponse,
  WebProductoRequest,
  WebProductoResponse,
  WebServicioRequest,
  WebServicioResponse,
} from '../models/web.model';

/** Recursos del panel con el mismo CRUD + publicación + un archivo (imagen/logo/PDF). */
export interface RecursoWeb<Res, Req> {
  /** Segmento de /api/web/{ruta}. */
  ruta: string;
  /** Segmento del archivo: "imagen", "logo" o "archivo". */
  archivo: 'imagen' | 'logo' | 'archivo';
  _tipos?: [Res, Req];
}

export const PRODUCTOS: RecursoWeb<WebProductoResponse, WebProductoRequest> = { ruta: 'productos', archivo: 'imagen' };
export const SERVICIOS: RecursoWeb<WebServicioResponse, WebServicioRequest> = { ruta: 'servicios', archivo: 'imagen' };
export const PARTNERS: RecursoWeb<WebPartnerResponse, WebPartnerRequest> = { ruta: 'partners', archivo: 'logo' };
export const CLIENTES: RecursoWeb<WebClienteReferenciaResponse, WebClienteReferenciaRequest> = {
  ruta: 'clientes',
  archivo: 'logo',
};
export const CASOS_EXITO: RecursoWeb<WebCasoExitoResponse, WebCasoExitoRequest> = {
  ruta: 'casos-exito',
  archivo: 'imagen',
};
export const DOCUMENTOS: RecursoWeb<WebDocumentoResponse, WebDocumentoRequest> = {
  ruta: 'documentos',
  archivo: 'archivo',
};

/**
 * Cliente HTTP del panel del sitio web (/api/web/**). Los permisos los valida
 * el backend: web_catalogo.* (productos, servicios), web_comercial.*
 * (partners, clientes, casos de éxito), web_novedad.* y web_documento.*.
 */
@Injectable({ providedIn: 'root' })
export class WebContenidoService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listar<Res, Req>(recurso: RecursoWeb<Res, Req>): Observable<Res[]> {
    return this.http.get<Res[]>(this.url(recurso.ruta));
  }

  crear<Res, Req>(recurso: RecursoWeb<Res, Req>, request: Req): Observable<Res> {
    return this.http.post<Res>(this.url(recurso.ruta), request);
  }

  editar<Res, Req>(recurso: RecursoWeb<Res, Req>, id: string, request: Req): Observable<Res> {
    return this.http.put<Res>(`${this.url(recurso.ruta)}/${id}`, request);
  }

  eliminar<Res, Req>(recurso: RecursoWeb<Res, Req>, id: string): Observable<void> {
    return this.http.delete<void>(`${this.url(recurso.ruta)}/${id}`);
  }

  cambiarPublicacion<Res, Req>(recurso: RecursoWeb<Res, Req>, id: string, publicado: boolean): Observable<Res> {
    return this.http.patch<Res>(`${this.url(recurso.ruta)}/${id}/publicacion`, { publicado });
  }

  subirArchivo<Res, Req>(recurso: RecursoWeb<Res, Req>, id: string, file: File): Observable<Res> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Res>(`${this.url(recurso.ruta)}/${id}/${recurso.archivo}`, formData);
  }

  /** Documentos no tienen DELETE de archivo: el PDF solo se reemplaza. */
  eliminarArchivo<Res, Req>(recurso: RecursoWeb<Res, Req>, id: string): Observable<Res> {
    return this.http.delete<Res>(`${this.url(recurso.ruta)}/${id}/${recurso.archivo}`);
  }

  // ---------- Novedades (paginadas y con ciclo de estado propio) ----------

  listarNovedades(estado: EstadoNovedad | null, pagina: number, tamano: number): Observable<PaginaResponse<WebNovedadResponse>> {
    const params: Record<string, string> = { pagina: String(pagina), tamano: String(tamano) };
    if (estado) params['estado'] = estado;
    return this.http.get<PaginaResponse<WebNovedadResponse>>(this.url('novedades'), { params });
  }

  crearNovedad(request: WebNovedadRequest): Observable<WebNovedadResponse> {
    return this.http.post<WebNovedadResponse>(this.url('novedades'), request);
  }

  editarNovedad(id: string, request: WebNovedadRequest): Observable<WebNovedadResponse> {
    return this.http.put<WebNovedadResponse>(`${this.url('novedades')}/${id}`, request);
  }

  cambiarEstadoNovedad(id: string, request: EstadoNovedadRequest): Observable<WebNovedadResponse> {
    return this.http.patch<WebNovedadResponse>(`${this.url('novedades')}/${id}/estado`, request);
  }

  eliminarNovedad(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url('novedades')}/${id}`);
  }

  subirPortadaNovedad(id: string, file: File): Observable<WebNovedadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<WebNovedadResponse>(`${this.url('novedades')}/${id}/imagen`, formData);
  }

  eliminarPortadaNovedad(id: string): Observable<WebNovedadResponse> {
    return this.http.delete<WebNovedadResponse>(`${this.url('novedades')}/${id}/imagen`);
  }

  listarCategorias(): Observable<WebCategoriaNovedadResponse[]> {
    return this.http.get<WebCategoriaNovedadResponse[]>(this.url('categorias-novedad'));
  }

  crearCategoria(request: WebCategoriaNovedadRequest): Observable<WebCategoriaNovedadResponse> {
    return this.http.post<WebCategoriaNovedadResponse>(this.url('categorias-novedad'), request);
  }

  editarCategoria(id: string, request: WebCategoriaNovedadRequest): Observable<WebCategoriaNovedadResponse> {
    return this.http.put<WebCategoriaNovedadResponse>(`${this.url('categorias-novedad')}/${id}`, request);
  }

  eliminarCategoria(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url('categorias-novedad')}/${id}`);
  }

  private url(ruta: string): string {
    return `${this.apiBaseUrl}/web/${ruta}`;
  }
}
