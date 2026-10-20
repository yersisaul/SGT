/**
 * Contratos reales del panel del sitio web público (Web*Controller,
 * /api/web/**). Las URL de imagen/archivo que devuelve el panel exigen JWT:
 * se cargan con ArchivoService.descargarBlob, nunca con <img src> directo.
 */

/** Columnas de auditoría comunes (AuditoriaWeb). */
interface AuditoriaWeb {
  fecha_creacion: string;
  fecha_actualizacion: string;
  nombre_usuario_actualiza: string | null;
}

export interface ModuloProducto {
  nombre: string;
  grupo: string | null;
  descripcion: string | null;
}

export interface WebProductoResponse extends AuditoriaWeb {
  id_producto: string;
  slug: string;
  nombre: string;
  subtitulo: string | null;
  descripcion_corta: string | null;
  descripcion: string | null;
  url_img: string | null;
  url_video: string | null;
  caracteristicas: string[] | null;
  modulos: ModuloProducto[] | null;
  destacado: boolean;
  orden: number;
  publicado: boolean;
}

export interface WebProductoRequest {
  nombre: string;
  slug?: string | null;
  subtitulo?: string | null;
  descripcion_corta?: string | null;
  descripcion?: string | null;
  url_video?: string | null;
  caracteristicas?: string[];
  modulos?: ModuloProducto[];
  destacado?: boolean;
  orden?: number;
  publicado?: boolean;
}

export interface WebServicioResponse extends AuditoriaWeb {
  id_servicio: string;
  slug: string;
  nombre: string;
  descripcion_corta: string | null;
  descripcion: string | null;
  /** Nombre de ícono de Font Awesome 6 sin prefijo (p. ej. "server"). */
  icono: string | null;
  url_img: string | null;
  caracteristicas: string[] | null;
  orden: number;
  publicado: boolean;
}

export interface WebServicioRequest {
  nombre: string;
  slug?: string | null;
  descripcion_corta?: string | null;
  descripcion?: string | null;
  icono?: string | null;
  caracteristicas?: string[];
  orden?: number;
  publicado?: boolean;
}

export type TipoPartner = 'TECNOLOGICO' | 'MARCA_COMPATIBLE';

export interface WebPartnerResponse extends AuditoriaWeb {
  id_partner: string;
  nombre: string;
  categoria: string | null;
  descripcion: string | null;
  url_logo: string | null;
  url_sitio: string | null;
  tipo: TipoPartner;
  orden: number;
  publicado: boolean;
}

export interface WebPartnerRequest {
  nombre: string;
  categoria?: string | null;
  descripcion?: string | null;
  url_sitio?: string | null;
  tipo?: TipoPartner;
  orden?: number;
  publicado?: boolean;
}

export interface WebClienteReferenciaResponse extends AuditoriaWeb {
  id_cliente_referencia: string;
  nombre: string;
  sector: string | null;
  url_logo: string | null;
  url_sitio: string | null;
  orden: number;
  publicado: boolean;
}

export interface WebClienteReferenciaRequest {
  nombre: string;
  sector?: string | null;
  url_sitio?: string | null;
  orden?: number;
  publicado?: boolean;
}

export interface MetricaCaso {
  valor: string;
  etiqueta: string;
}

export interface WebCasoExitoResponse extends AuditoriaWeb {
  id_caso_exito: string;
  slug: string;
  titulo: string;
  resumen: string | null;
  contenido: string | null;
  url_img: string | null;
  url_externa: string | null;
  /** yyyy-MM-dd */
  fecha_caso: string | null;
  metricas: MetricaCaso[] | null;
  id_cliente_referencia: string | null;
  nombre_cliente: string | null;
  id_producto: string | null;
  nombre_producto: string | null;
  destacado: boolean;
  orden: number;
  publicado: boolean;
}

export interface WebCasoExitoRequest {
  titulo: string;
  slug?: string | null;
  resumen?: string | null;
  contenido?: string | null;
  url_externa?: string | null;
  fecha_caso?: string | null;
  metricas?: MetricaCaso[];
  /** Se reemplaza siempre: null = sin vínculo. */
  id_cliente_referencia: string | null;
  /** Se reemplaza siempre: null = sin vínculo. */
  id_producto: string | null;
  destacado?: boolean;
  orden?: number;
  publicado?: boolean;
}

export type EstadoNovedad = 'BORRADOR' | 'PUBLICADO' | 'ARCHIVADO';

export interface WebNovedadResponse extends AuditoriaWeb {
  id_novedad: string;
  slug: string;
  titulo: string;
  resumen: string | null;
  contenido: string | null;
  url_portada: string | null;
  estado: EstadoNovedad;
  fecha_publicacion: string | null;
  /** PUBLICADO con fecha cumplida; PUBLICADO con fecha futura = programada. */
  visible_en_sitio: boolean;
  id_categoria_novedad: string | null;
  nombre_categoria: string | null;
  nombre_autor: string | null;
  destacado: boolean;
}

export interface WebNovedadRequest {
  titulo: string;
  slug?: string | null;
  resumen?: string | null;
  contenido?: string | null;
  /** Se reemplaza siempre: null = sin categoría. */
  id_categoria_novedad: string | null;
  destacado?: boolean;
}

export interface EstadoNovedadRequest {
  estado: EstadoNovedad;
  /** ISO-8601; futura = programada. */
  fecha_publicacion?: string | null;
}

export interface WebCategoriaNovedadResponse {
  id_categoria_novedad: string;
  nombre: string;
  slug: string;
  orden: number;
}

export interface WebCategoriaNovedadRequest {
  nombre: string;
  slug?: string | null;
  orden?: number;
}

export type TipoDocumentoWeb = 'BROCHURE' | 'FICHA_TECNICA' | 'PRESENTACION';

export interface WebDocumentoResponse extends AuditoriaWeb {
  id_documento: string;
  titulo: string;
  descripcion: string | null;
  tipo: TipoDocumentoWeb;
  /** null mientras no se suba el PDF. */
  url_archivo: string | null;
  nombre_original: string | null;
  tamano_bytes: number | null;
  id_producto: string | null;
  nombre_producto: string | null;
  orden: number;
  publicado: boolean;
}

export interface WebDocumentoRequest {
  titulo: string;
  descripcion?: string | null;
  tipo?: TipoDocumentoWeb;
  /** Se reemplaza siempre: null = sin producto. */
  id_producto: string | null;
  orden?: number;
  publicado?: boolean;
}

export interface PaginaResponse<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  total_elementos: number;
  total_paginas: number;
}

export const TIPO_PARTNER_LABEL: Record<TipoPartner, string> = {
  TECNOLOGICO: 'Alianza tecnológica',
  MARCA_COMPATIBLE: 'Marca compatible',
};

export const TIPO_DOCUMENTO_LABEL: Record<TipoDocumentoWeb, string> = {
  BROCHURE: 'Brochure',
  FICHA_TECNICA: 'Ficha técnica',
  PRESENTACION: 'Presentación',
};

export const ESTADO_NOVEDAD_LABEL: Record<EstadoNovedad, string> = {
  BORRADOR: 'Borrador',
  PUBLICADO: 'Publicada',
  ARCHIVADO: 'Archivada',
};
